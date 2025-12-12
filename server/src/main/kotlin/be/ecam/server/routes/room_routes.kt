package be.ecam.server.routes

import be.ecam.server.models.RoomWriteRequest
import be.ecam.server.services.RoomService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.roomRoutes() {

    route("/rooms") {


        get {
            call.respond(RoomService.getAllRooms())
        }


        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val room = RoomService.getRoomById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Room not found")

            call.respond(room)
        }


        get("by-code/{roomId}") {
            val roomId = call.parameters["roomId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "roomId missing")

            call.respond(RoomService.getRoomsByRoomId(roomId))
        }


        post {
            val req = call.receive<RoomWriteRequest>()
            val created = RoomService.createRoom(req)
            call.respond(HttpStatusCode.Created, created)
        }


        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<RoomWriteRequest>()
            val updated = RoomService.updateRoom(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Room not found")

            call.respond(updated)
        }


        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = RoomService.deleteRoom(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Room not found")
        }
    }
}
