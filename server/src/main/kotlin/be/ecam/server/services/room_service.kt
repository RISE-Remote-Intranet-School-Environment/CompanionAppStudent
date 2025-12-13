package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object RoomService {

    //  GET all rooms
    fun getAllRooms(): List<RoomDTO> = transaction {
        RoomsTable
            .selectAll()
            .map { it.toRoomDTO() }
    }

    //  GET by DB id
    fun getRoomById(id: Int): RoomDTO? = transaction {
        RoomsTable
            .selectAll()
            .where { RoomsTable.id eq id }
            .singleOrNull()
            ?.toRoomDTO()
    }

    //  GET by logical roomId (ex: "B01", "A3-05")
    fun getRoomsByRoomId(roomId: String): List<RoomDTO> = transaction {
        RoomsTable
            .selectAll()
            .where { RoomsTable.roomId eq roomId }
            .map { it.toRoomDTO() }
    }

    //  CREATE
    fun createRoom(req: RoomWriteRequest): RoomDTO = transaction {
        val newId = RoomsTable.insertAndGetId { row ->
            row[RoomsTable.roomId] = req.roomId
            row[RoomsTable.type] = req.type
            row[RoomsTable.batiment] = req.batiment
            row[RoomsTable.etage] = req.etage
        }

        RoomsTable
            .selectAll()
            .where { RoomsTable.id eq newId }
            .single()
            .toRoomDTO()
    }

    //  UPDATE
    fun updateRoom(id: Int, req: RoomWriteRequest): RoomDTO? = transaction {
        val updated = RoomsTable.update({ RoomsTable.id eq id }) { row ->
            row[RoomsTable.roomId] = req.roomId
            row[RoomsTable.type] = req.type
            row[RoomsTable.batiment] = req.batiment
            row[RoomsTable.etage] = req.etage
        }

        if (updated == 0) return@transaction null

        RoomsTable
            .selectAll()
            .where { RoomsTable.id eq id }
            .singleOrNull()
            ?.toRoomDTO()
    }

    //  DELETE
    fun deleteRoom(id: Int): Boolean = transaction {
        RoomsTable.deleteWhere { RoomsTable.id eq id } > 0
    }
}
