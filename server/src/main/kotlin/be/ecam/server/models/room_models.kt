package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table pour les salles
object RoomsTable : IntIdTable("rooms") {
    val roomId = varchar("room_id", 50)
    val type = varchar("type", 50)
    val batiment = varchar("batiment", 50).nullable()
    val etage = varchar("etage", 50).nullable()
}

// DTO pour exposer les salles au front
@Serializable
data class RoomDTO(
    val id: Int,
    val roomId: String,
    val type: String,
    val batiment: String?,
    val etage: String?
)

// mapper ResultRow -> DTO
fun ResultRow.toRoomDTO() = RoomDTO(
    id = this[RoomsTable.id].value,
    roomId = this[RoomsTable.roomId],
    type = this[RoomsTable.type],
    batiment = this[RoomsTable.batiment],
    etage = this[RoomsTable.etage]
)

// DTO pour écriture (création et update complet)
@Serializable
data class RoomWriteRequest(
    val roomId: String,
    val type: String,
    val batiment: String? = null,
    val etage: String? = null
)