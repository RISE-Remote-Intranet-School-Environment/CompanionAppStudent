package be.ecam.server.services

import be.ecam.server.models.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CatalogService {

    // --- DTOs pour parser ecam_formations_2025.json ---

    @Serializable
    data class FormationsFile(
        val year: String,
        val generated_at: String,
        val source: String,
        val formations: List<FormationJson>
    )

    @Serializable
    data class FormationJson(
        val id: String,
        val name: String,
        val source_url: String? = null,
        val image_url: String? = null,
        val blocks: List<BlockJson>
    )

    @Serializable
    data class BlockJson(
        val name: String,
        val courses: List<BlockCourseJson>
    )

    @Serializable
    data class BlockCourseJson(
        val code: String,
        val title: String,
        val credits: Int,
        val periods: List<String>,
        val details_url: String
    )

    // --- SEED depuis JSON ---

    fun seedFormationsFromJson() {
        val resource = CatalogService::class.java.classLoader
            .getResource("files/ecam_formations_2025.json")
            ?: error("Resource 'files/ecam_formations_2025.json' introuvable dans le classpath")

        val text = resource.readText()
        val json = Json { ignoreUnknownKeys = true }
        val file = json.decodeFromString<FormationsFile>(text)

        transaction {
            file.formations.forEach { f ->

                val formation = Formation.find { FormationTable.slug eq f.id }.firstOrNull()
                    ?: Formation.new {
                        slug = f.id
                        name = f.name
                        sourceUrl = f.source_url ?: ""
                        imageUrl = f.image_url
                    }

                // mise à jour éventuellement
                formation.imageUrl = f.image_url ?: formation.imageUrl

                f.blocks.forEach { b ->

                    val block = Block.find {
                        (BlockTable.name eq b.name) and (BlockTable.formation eq formation.id)
                    }.firstOrNull()
                        ?: Block.new {
                            name = b.name
                            this.formation = formation
                        }

                    b.courses.forEach { c ->

                        val existing = Course.find {
                            (CourseTable.code eq c.code) and (CourseTable.formation eq formation.id)
                        }.firstOrNull()

                        val course = existing ?: Course.new {}

                        course.code = c.code
                        course.title = c.title
                        course.credits = c.credits
                        course.periods = c.periods.joinToString(",")
                        course.detailsUrl = c.details_url
                        course.formation = formation
                        course.bloc = block.name
                        course.blockRef = block
                    }
                }
            }
        }
    }

    // --- LECTURE : formations pour l’API ---

    fun getAllFormations(): List<FormationDTO> = transaction {
        Formation.all().map {
            FormationDTO(
                id = it.id.value,
                slug = it.slug,
                name = it.name,
                imageUrl = it.imageUrl
            )
        }
    }

    // --- LECTURE : cours par formation (via slug) ---

    fun getCoursesByFormationSlug(slug: String): List<CourseDTO> = transaction {
        val formation = Formation.find { FormationTable.slug eq slug }.firstOrNull()
            ?: return@transaction emptyList()

        Course.find { CourseTable.formation eq formation.id }.map {
            val blocName = it.blockRef?.name ?: it.bloc
            CourseDTO(
                id = it.id.value,
                code = it.code,
                title = it.title,
                credits = it.credits,
                periods = it.periods,
                bloc = blocName,
                detailsUrl = it.detailsUrl,
                formationSlug = formation.slug
            )
        }
    }

    // --- FORMATIONS CRUD ---

    fun getFormationById(id: Int): FormationDTO? = transaction {
        Formation.findById(id)?.let {
            FormationDTO(
                id = it.id.value,
                slug = it.slug,
                name = it.name,
                imageUrl = it.imageUrl
            )
        }
    }

    fun getFormationBySlug(slug: String): FormationDTO? = transaction {
        Formation.find { FormationTable.slug eq slug }.firstOrNull()?.let {
            FormationDTO(
                id = it.id.value,
                slug = it.slug,
                name = it.name,
                imageUrl = it.imageUrl
            )
        }
    }

    fun createFormation(req: FormationWriteRequest): FormationDTO = transaction {
        val formation = Formation.new {
            slug = req.slug
            name = req.name
            sourceUrl = req.sourceUrl
            imageUrl = req.imageUrl
        }
        FormationDTO(
            id = formation.id.value,
            slug = formation.slug,
            name = formation.name,
            imageUrl = formation.imageUrl
        )
    }

    fun updateFormation(id: Int, req: FormationWriteRequest): FormationDTO? = transaction {
        val formation = Formation.findById(id) ?: return@transaction null

        formation.slug = req.slug
        formation.name = req.name
        formation.sourceUrl = req.sourceUrl
        formation.imageUrl = req.imageUrl

        FormationDTO(
            id = formation.id.value,
            slug = formation.slug,
            name = formation.name,
            imageUrl = formation.imageUrl
        )
    }

    fun deleteFormation(id: Int): Boolean = transaction {
        val formation = Formation.findById(id) ?: return@transaction false
        formation.delete()
        true
    }

    // --- BLOCKS CRUD ---

    fun createBlock(req: BlockWriteRequest): BlockDTO = transaction {
        val formation = Formation.findById(req.formationId)
            ?: error("Formation ${req.formationId} introuvable")

        val block = Block.new {
            name = req.name
            this.formation = formation
        }

        BlockDTO(
            id = block.id.value,
            name = block.name,
            formationId = block.formation.id.value
        )
    }

    fun updateBlock(id: Int, req: BlockWriteRequest): BlockDTO? = transaction {
        val block = Block.findById(id) ?: return@transaction null
        val formation = Formation.findById(req.formationId)
            ?: error("Formation ${req.formationId} introuvable")

        block.name = req.name
        block.formation = formation

        BlockDTO(
            id = block.id.value,
            name = block.name,
            formationId = block.formation.id.value
        )
    }

    fun deleteBlock(id: Int): Boolean = transaction {
        val block = Block.findById(id) ?: return@transaction false
        block.delete()
        true
    }

    // --- Helper pour mapper un Course en CourseDTO ---

    private fun Course.toDto(): CourseDTO {
        val formation = this.formation
        val blocName = this.blockRef?.name ?: this.bloc

        return CourseDTO(
            id = this.id.value,
            code = this.code,
            title = this.title,
            credits = this.credits,
            periods = this.periods,
            bloc = blocName,
            detailsUrl = this.detailsUrl,
            formationSlug = formation?.slug
        )
    }

    // --- COURSES CRUD / LECTURE ---

    fun getCourseById(id: Int): CourseDTO? = transaction {
        Course.findById(id)?.toDto()
    }

    fun getCourseByCode(code: String): CourseDTO? = transaction {
        Course.find { CourseTable.code eq code }.firstOrNull()?.toDto()
    }

    fun createCourse(req: CourseWriteRequest): CourseDTO = transaction {
        val formation = req.formationId?.let { fid ->
            Formation.findById(fid) ?: error("Formation $fid introuvable")
        }
        val block = req.blockId?.let { bid ->
            Block.findById(bid) ?: error("Block $bid introuvable")
        }

        val course = Course.new {
            code = req.code
            title = req.title
            credits = req.credits
            periods = req.periods
            detailsUrl = req.detailsUrl
            mandatory = req.mandatory
            bloc = req.bloc
            program = req.program
            language = req.language
            this.formation = formation
            this.blockRef = block
        }

        course.toDto()
    }

    fun updateCourse(id: Int, req: CourseWriteRequest): CourseDTO? = transaction {
        val course = Course.findById(id) ?: return@transaction null

        val formation = req.formationId?.let { fid ->
            Formation.findById(fid) ?: error("Formation $fid introuvable")
        }
        val block = req.blockId?.let { bid ->
            Block.findById(bid) ?: error("Block $bid introuvable")
        }

        course.code = req.code
        course.title = req.title
        course.credits = req.credits
        course.periods = req.periods
        course.detailsUrl = req.detailsUrl
        course.mandatory = req.mandatory
        course.bloc = req.bloc
        course.program = req.program
        course.language = req.language
        course.formation = formation
        course.blockRef = block

        course.toDto()
    }

    fun deleteCourse(id: Int): Boolean = transaction {
        val course = Course.findById(id) ?: return@transaction false
        course.delete()
        true
    }

    // =====================
    //  JSON course details
    // =====================

    @Serializable
    private data class CourseSectionsJson(
        @SerialName("Contribution au programme")
        val contribution: String? = null,
        @SerialName("Acquis d’apprentissage spécifiques")
        val learningOutcomes: String? = null,
        @SerialName("Description du contenu")
        val content: String? = null,
        @SerialName("Méthodes d'enseignement")
        val teachingMethods: String? = null,
        @SerialName("Méthodes d'évaluation")
        val evaluationMethods: String? = null,
        @SerialName("Support de cours")
        val courseMaterial: String? = null,
        @SerialName("Bibliographie")
        val bibliography: String? = null
    )

    @Serializable
    private data class CourseDetailsJson(
        val details_url: String? = null,
        val code: String,
        val title: String,
        val mandatory: Boolean? = null,
        val bloc: String? = null,
        val program: String? = null,
        val credits: String? = null,
        val hours: String? = null,
        val responsable: String? = null,
        val teachers: List<String>? = null,
        val language: String? = null,
        val organized_activities: List<OrganizedActivityJson>? = null,
        val evaluated_activities: List<EvaluatedActivityJson>? = null,
        val sections: CourseSectionsJson? = null,
        val formation: String? = null,
        val block: String? = null
    )

    @Serializable
    private data class OrganizedActivityJson(
        val code: String,
        val title: String,
        val hours_Q1: String? = null,
        val hours_Q2: String? = null,
        val teachers: List<String>? = null,
        val language: String? = null
    )

    @Serializable
    private data class EvaluatedActivityJson(
        val code: String,
        val title: String,
        val weight: String? = null,
        val type_Q1: String? = null,
        val type_Q2: String? = null,
        val type_Q3: String? = null,
        val teachers: List<String>? = null,
        val language: String? = null,
        val linked_activities: List<String>? = null
    )

    // ============================================================
    // SEED course details
    // ============================================================
    fun seedCourseDetailsFromJson() {
        val resource = CatalogService::class.java.classLoader
            .getResource("files/ecam_courses_details_2025.json")
            ?: error("Resource 'files/ecam_courses_details_2025.json' introuvable dans le classpath")

        val text = resource.readText()
        val json = Json { ignoreUnknownKeys = true }

        val detailsList = json.decodeFromString<List<CourseDetailsJson>>(text)

        transaction {
            detailsList.forEach { d ->

                // retrouver le cours (ex: 1bach10, 4eore40, etc.)
                val course = Course.find { CourseTable.code eq d.code }.firstOrNull()
                    ?: return@forEach

                // collecter TOUS les profs dans teachersRaw
                val teachersAll = buildList {
                    d.teachers?.let { addAll(it) }
                    d.organized_activities?.forEach { oa ->
                        oa.teachers?.let { addAll(it) }
                    }
                    d.evaluated_activities?.forEach { ea ->
                        ea.teachers?.let { addAll(it) }
                    }
                }.distinct()

                val teachersRaw = if (teachersAll.isEmpty()) null else teachersAll.joinToString("; ")

                val sections = d.sections

                // upsert : si details déjà existants → update
                val existing = CourseDetails.find { CourseDetailsTable.course eq course.id }.firstOrNull()

                if (existing == null) {
                    CourseDetails.new {
                        this.course = course
                        responsable = d.responsable
                        this.teachersRaw = teachersRaw
                        contribution = sections?.contribution
                        learningOutcomes = sections?.learningOutcomes
                        content = sections?.content
                        teachingMethods = sections?.teachingMethods
                        evaluationMethods = sections?.evaluationMethods
                        courseMaterial = sections?.courseMaterial
                        bibliography = sections?.bibliography
                    }
                } else {
                    existing.apply {
                        responsable = d.responsable
                        this.teachersRaw = teachersRaw
                        contribution = sections?.contribution
                        learningOutcomes = sections?.learningOutcomes
                        content = sections?.content
                        teachingMethods = sections?.teachingMethods
                        evaluationMethods = sections?.evaluationMethods
                        courseMaterial = sections?.courseMaterial
                        bibliography = sections?.bibliography
                    }
                }
            }
        }
    }

    // ============================================================
    // Helpers CourseDetails -> DTO
    // ============================================================

    private fun CourseDetails.toDto(): CourseDetailsDTO =
        CourseDetailsDTO(
            id = id.value,
            courseId = course.id.value,
            courseCode = course.code,
            titre = course.title,
            responsable = responsable,
            teachers = teachersList(),
            contribution = contribution,
            learningOutcomes = learningOutcomes,
            content = content,
            teachingMethods = teachingMethods,
            evaluationMethods = evaluationMethods,
            courseMaterial = courseMaterial,
            bibliography = bibliography
        )

    // ============================================================
    // READ : récupérer les détails d’un cours
    // ============================================================

    // Par CODE (utile si tu veux /api/courses/code/{code}/details)
    fun getCourseDetailsByCode(code: String): CourseDetailsDTO? = transaction {
        val course = Course.find { CourseTable.code eq code }.firstOrNull() ?: return@transaction null
        CourseDetails
            .find { CourseDetailsTable.course eq course.id }
            .firstOrNull()
            ?.toDto()
    }

    // Par ID (ce que ta route /api/courses/{id}/details utilise)
    fun getCourseDetailsByCourseId(courseId: Int): CourseDetailsDTO? = transaction {
        val course = Course.findById(courseId) ?: return@transaction null
        CourseDetails
            .find { CourseDetailsTable.course eq course.id }
            .firstOrNull()
            ?.toDto()
    }
}
