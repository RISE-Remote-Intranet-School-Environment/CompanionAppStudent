package be.ecam.server.services

import be.ecam.server.models.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CatalogService {

    private fun warn(msg: String) {
        println("[CatalogService][WARN] $msg")
    }
 
   
    // JSON formations format
   
    // JSON formations format
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

    // SEED from JSON 

    fun seedFormationsFromJson() {
        val resource = CatalogService::class.java.classLoader
            .getResource("files/ecam_formations_2025.json")
            ?: error("Resource 'files/ecam_formations_2025.json' not found in classpath")

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


                // update if needed
                formation.imageUrl = f.image_url ?: formation.imageUrl

                f.blocks.forEach { b ->

                    val block = Block.find { BlockTable.name eq b.name }.firstOrNull()
                        ?: Block.new {
                            blocId = b.name
                            name = b.name
                            formationIds = f.id
                        }
                    val current = block.formationIds?.split(";")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
                    current.add(f.id)
                    block.formationIds = current.joinToString(";")

                    b.courses.forEach { c ->

                        val existing = Course.find {
                            (CourseTable.code eq c.code) and (CourseTable.formation eq f.id)
                        }.firstOrNull()

                        val course = existing ?: Course.new {}

                        course.code = c.code
                        course.title = c.title
                        course.credits = c.credits.toString()
                        course.periods = c.periods.joinToString(",")
                        course.detailsUrl = c.details_url
                        course.formationId = f.id
                        course.bloc = block.blocId
                    }
                }
            }
        }
    }

    // read all formations

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

    // read courses by formation slug

    fun getCoursesByFormationSlug(slug: String): List<CourseDTO> = transaction {
        val formation = Formation.find { FormationTable.slug eq slug }.firstOrNull()
            ?: run {
                warn("Formation not found for slug=$slug")
                return@transaction emptyList()
            }

        Course.find { CourseTable.formation eq formation.slug }.map {
            val blocName = it.bloc
            CourseDTO(
                id = it.id.value,
                code = it.code,
                title = it.title,
                credits = it.credits?.toIntOrNull() ?: 0,
                periods = it.periods,
                bloc = blocName,
                detailsUrl = it.detailsUrl,
                formationSlug = it.formationId
            )
        }
    }

    // formations CRUD

    fun getFormationById(id: Int): FormationDTO? = transaction {
        Formation.findById(id)?.let {
            FormationDTO(
                id = it.id.value,
                slug = it.slug,
                name = it.name,
                imageUrl = it.imageUrl
            )
        } ?: run {
            warn("Formation not found for id=$id")
            null
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
        } ?: run {
            warn("Formation not found for slug=$slug")
            null
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
        val formation = Formation.findById(id) ?: return@transaction run {
            warn("Cannot update formation: id=$id not found")
            null
        }

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
        val formation = Formation.findById(id) ?: return@transaction run {
            warn("Cannot delete formation: id=$id not found")
            false
        }
        formation.delete()
        true
    }

    // blocks CRUD

    fun createBlock(req: BlockWriteRequest): BlockDTO = transaction {
        val block = Block.new {
            blocId = req.name
            name = req.name
            formationIds = req.formationIds.joinToString(";")
        }

        BlockDTO(
            id = block.id.value,
            name = block.name,
            formationIds = req.formationIds
        )
    }

    fun updateBlock(id: Int, req: BlockWriteRequest): BlockDTO? = transaction {
        val block = Block.findById(id) ?: return@transaction run {
            warn("Cannot update block: id=$id not found")
            null
        }
        block.blocId = req.name
        block.name = req.name
        block.formationIds = req.formationIds.joinToString(";")

        BlockDTO(
            id = block.id.value,
            name = block.name,
            formationIds = req.formationIds
        )
    }

    fun deleteBlock(id: Int): Boolean = transaction {
        val block = Block.findById(id) ?: return@transaction run {
            warn("Cannot delete block: id=$id not found")
            false
        }
        block.delete()
        true
    }

    // --- Helper to map a Course to CourseDTO ---

    private fun Course.toDto(): CourseDTO {
        val blocName = this.bloc

        return CourseDTO(
            id = this.id.value,
            code = this.code,
            title = this.title,
            credits = this.credits?.toIntOrNull() ?: 0,
            periods = this.periods,
            bloc = blocName,
            detailsUrl = this.detailsUrl,
            formationSlug = this.formationId
        )
    }

    // courses CRUD

    fun getCourseById(id: Int): CourseDTO? = transaction {
        Course.findById(id)?.toDto() ?: run {
            warn("Course not found for id=$id")
            null
        }
    }

    fun getCourseByCode(code: String): CourseDTO? = transaction {
        Course.find { CourseTable.code eq code }.firstOrNull()?.toDto() ?: run {
            warn("Course not found for code=$code")
            null
        }
    }

    fun createCourse(req: CourseWriteRequest): CourseDTO = transaction {
        val course = Course.new {
            code = req.code
            title = req.title
            credits = req.credits.toString()
            periods = req.periods
            detailsUrl = req.detailsUrl
            mandatory = req.mandatory.toString()
            bloc = req.bloc ?: req.blockId?.toString()
            language = req.language
            formationId = req.formationId?.toString()
        }

        course.toDto()
    }

    fun updateCourse(id: Int, req: CourseWriteRequest): CourseDTO? = transaction {
        val course = Course.findById(id) ?: return@transaction run {
            warn("Cannot update course: id=$id not found")
            null
        }

        course.code = req.code
        course.title = req.title
        course.credits = req.credits.toString()
        course.periods = req.periods
        course.detailsUrl = req.detailsUrl
        course.mandatory = req.mandatory.toString()
        course.bloc = req.bloc ?: req.blockId?.toString()
        course.language = req.language
        course.formationId = req.formationId?.toString()

        course.toDto()
    }

    fun deleteCourse(id: Int): Boolean = transaction {
        val course = Course.findById(id) ?: return@transaction run {
            warn("Cannot delete course: id=$id not found")
            false
        }
        course.delete()
        true
    }

  
    // json course details format
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

    // SEED CourseDetails from JSON
    fun seedCourseDetailsFromJson() {
        val resource = CatalogService::class.java.classLoader
            .getResource("files/ecam_courses_details_2025.json")
            ?: error("Resource 'files/ecam_courses_details_2025.json' not found in classpath")
        val text = resource.readText()
        val json = Json { ignoreUnknownKeys = true }

        val detailsList = json.decodeFromString<List<CourseDetailsJson>>(text)

        transaction {
            detailsList.forEach { d ->

                // return if course not found
                val course = Course.find { CourseTable.code eq d.code }.firstOrNull()
                    ?: run {
                        warn("Skipping course details seed: course code ${d.code} not found")
                        return@forEach
                    }

                // collect all teachers into teachersRaw
                val teachersAll = buildList {
                    d.teachers?.let { addAll(it) }
                    d.organized_activities?.forEach { oa ->
                        oa.teachers?.let { addAll(it) }
                    }
                    d.evaluated_activities?.forEach { ea ->
                        ea.teachers?.let { addAll(it) }
                    }
                }.distinct()

                val teachersRawStr = if (teachersAll.isEmpty()) null else teachersAll.joinToString("; ")

                val sections = d.sections

                // upsert : if details already exist : update
                val existing = CourseDetails.find { CourseDetailsTable.courseCode eq course.code }.firstOrNull()

                val updater: CourseDetails.() -> Unit = {
                    courseCode = course.code
                    responsable = d.responsable
                    teachersRaw = teachersRawStr
                    contribution = sections?.contribution
                    learningOutcomes = sections?.learningOutcomes
                    content = sections?.content
                    teachingMethods = sections?.teachingMethods
                    evaluationMethods = sections?.evaluationMethods
                    courseMaterial = sections?.courseMaterial
                    bibliography = sections?.bibliography
                    blocId = course.bloc
                }

                if (existing == null) {
                    CourseDetails.new { updater() }
                } else {
                    existing.apply(updater)
                }
            }
        }
    }

    // mapper CourseDetails : CourseDetailsDTO

    private fun CourseDetails.toDto(): CourseDetailsDTO =
        CourseDetailsDTO(
            id = id.value,
            courseId = Course.find { CourseTable.code eq courseCode }.firstOrNull()?.id?.value,
            courseCode = courseCode,
            titre = Course.find { CourseTable.code eq courseCode }.firstOrNull()?.title,
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

    // read course details

    // for course code
    fun getCourseDetailsByCode(code: String): CourseDetailsDTO? = transaction {
        CourseDetails
            .find { CourseDetailsTable.courseCode eq code }
            .firstOrNull()
            ?.toDto()
            ?: run {
                warn("Course details not found for code=$code")
                null
            }
    }

    // for course ID (used by your route /api/courses/{id}/details)
    fun getCourseDetailsByCourseId(courseId: Int): CourseDetailsDTO? = transaction {
        val course = Course.findById(courseId) ?: return@transaction run {
            warn("Course not found for id=$courseId when fetching details")
            null
        }
        CourseDetails
            .find { CourseDetailsTable.courseCode eq course.code }
            .firstOrNull()
            ?.toDto()
            ?: run {
                warn("Course details not found for course code=${course.code}")
                null
            }
    }
}
