package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow


// Table pour les blocs
object BlocsTable : IntIdTable("blocs") {
    val blocId = varchar("bloc_id", 50)        // ex: "B1", "B2"...
    val name = varchar("name", 255)
    val formationIds = text("formation_ids")   // JSON array ou liste de formations
}

// DTO pour exposer les blocs au front
@Serializable
data class BlocDTO(
    val id: Int,
    val blocId: String,
    val name: String,
    val formationIds: String
)

// mapper ResultRow -> DTO
fun ResultRow.toBlocDTO() = BlocDTO(
    id = this[BlocsTable.id].value,
    blocId = this[BlocsTable.blocId],
    name = this[BlocsTable.name],
    formationIds = this[BlocsTable.formationIds]
)

/// DTO pour écriture (création et update complet)
@Serializable
data class BlocWriteRequest(
    val blocId: String,
    val name: String,
    val formationIds: String
)

