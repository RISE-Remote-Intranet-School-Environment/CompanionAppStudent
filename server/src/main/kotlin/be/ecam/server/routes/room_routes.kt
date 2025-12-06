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

        // 🔹 GET /api/rooms
        get {
            call.respond(RoomService.getAllRooms())
        }

        // 🔹 GET /api/rooms/{id}  (id DB)
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val room = RoomService.getRoomById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Room not found")

            call.respond(room)
        }

        // 🔹 GET /api/rooms/by-code/{roomId}
        get("by-code/{roomId}") {
            val roomId = call.parameters["roomId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "roomId missing")

            call.respond(RoomService.getRoomsByRoomId(roomId))
        }

        // 🔹 POST /api/rooms
        post {
            val req = call.receive<RoomWriteRequest>()
            val created = RoomService.createRoom(req)
            call.respond(HttpStatusCode.Created, created)
        }

        // 🔹 PUT /api/rooms/{id}
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<RoomWriteRequest>()
            val updated = RoomService.updateRoom(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Room not found")

            call.respond(updated)
        }

        // 🔹 DELETE /api/rooms/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = RoomService.deleteRoom(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Room not found")
        }
    }
}
