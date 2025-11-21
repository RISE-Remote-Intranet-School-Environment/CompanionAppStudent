package be.ecam.server.services

import be.ecam.server.models.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
                    }

                f.blocks.forEach { b ->
                    val block = Block.new {
                        name = b.name
                        this.formation = formation
                    }

                    b.courses.forEach { c ->
                        val existing = Course.find { CourseTable.code eq c.code }.firstOrNull()
                        if (existing == null) {
                            Course.new {
                                code = c.code
                                title = c.title
                                credits = c.credits
                                periods = c.periods.joinToString(",")
                                detailsUrl = c.details_url
                                this.formation = formation
                                this.blockRef = block
                            }
                        } else {
                            // ici tu pourrais mettre à jour des champs si tu veux
                            // existing.title = c.title
                        }
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
                name = it.name
            )
        }
    }

    // --- 3) LECTURE : cours par formation (via slug) ---

    fun getCoursesByFormationSlug(slug: String): List<CourseDTO> = transaction {
        val formation = Formation.find { FormationTable.slug eq slug }.firstOrNull()
            ?: return@transaction emptyList()

        Course.find { CourseTable.formation eq formation.id }.map {
            CourseDTO(
                id = it.id.value,
                code = it.code,
                title = it.title,
                credits = it.credits,
                bloc = it.bloc,
                formationSlug = formation.slug
            )
        }
    }
}
