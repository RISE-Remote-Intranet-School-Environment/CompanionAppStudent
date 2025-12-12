package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table pour les formations
object FormationsTable : IntIdTable("formations") {
    val formationId = varchar("formation_id", 50).uniqueIndex()  
    val name = varchar("name", 255)
    val sourceUrl = varchar("source_url", 512)
    val imageUrl = varchar("image_url", 512).nullable()
}

// DTO pour exposer les formations au front
@Serializable
data class FormationDTO(
    val id: Int,
    val formationId: String,
    val name: String,
    val sourceUrl: String,
    val imageUrl: String?
)

// mapper ResultRow -> DTO
fun ResultRow.toFormationDTO() = FormationDTO(
    id = this[FormationsTable.id].value,
    formationId = this[FormationsTable.formationId],
    name = this[FormationsTable.name],
    sourceUrl = this[FormationsTable.sourceUrl],
    imageUrl = this[FormationsTable.imageUrl]
)


// DTO pour écriture (création et update complet)
@Serializable
data class FormationWriteRequest(
    val formationId: String,
    val name: String,
    val sourceUrl: String,
    val imageUrl: String? = null
)
