package be.ecam.server.services

import be.ecam.server.models.*
import kotlinx.serialization.Serializable
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
        val id: String,            // "automatisation"
        val name: String,          // "Automatisation"
        val source_url: String? = null,
        val image_url: String? = null,
        val blocks: List<BlockJson>
    )

    @Serializable
    data class BlockJson(
        val name: String,          // "Bloc 1"
        val courses: List<BlockCourseJson>
    )

    @Serializable
    data class BlockCourseJson(
        val code: String,          // "1bach10"
        val title: String,         // "Chimie"
        val credits: Int,
        val periods: List<String>, // ["Q1"], ["Q1","Q2"]
        val details_url: String
    )

    // --- 1) SEED : remplir la DB à partir du JSON formations ---

    fun seedFormationsFromJson() {
        val resource = CatalogService::class.java.classLoader
            .getResource("files/ecam_formations_2025.json")
            ?: error("Resource 'files/ecam_formations_2025.json' introuvable dans le classpath")

        val text = resource.readText()

        val json = Json { ignoreUnknownKeys = true }
        val file = json.decodeFromString<FormationsFile>(text)

        transaction {
            file.formations.forEach { f ->

                // Formation : si déjà présente, on la réutilise
                val formation = Formation.find { FormationTable.slug eq f.id }.firstOrNull()
                    ?: Formation.new {
                        slug = f.id
                        name = f.name
                        sourceUrl = f.source_url ?: ""
                        imageUrl = f.image_url
                    }
                formation.imageUrl = f.image_url ?: formation.imageUrl

                f.blocks.forEach { b ->
                    val block = Block.find { (BlockTable.name eq b.name) and (BlockTable.formation eq formation.id) }
                        .firstOrNull()
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

    // --- 2) LECTURE : formations pour l’API ---

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

    // --- 3) LECTURE : cours par formation (via slug) ---

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
}
