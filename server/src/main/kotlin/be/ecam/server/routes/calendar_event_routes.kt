package be.ecam.server.routes

import be.ecam.server.models.CalendarEventWriteRequest
import be.ecam.server.services.CalendarService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.calendarRoutes() {

    route("/calendar") {

        get {
            call.respond(CalendarService.getAllEvents())
        }

        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val event = CalendarService.getEventById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound)

            call.respond(event)
        }

        get("by-date/{date}") {
            val date = call.parameters["date"]!!
            call.respond(CalendarService.getEventsByDate(date))
        }

        get("by-group/{group}") {
            val group = call.parameters["group"]!!
            call.respond(CalendarService.getEventsByGroup(group))
        }

        post {
            val req = call.receive<CalendarEventWriteRequest>()
            call.respond(HttpStatusCode.Created, CalendarService.createEvent(req))
        }

        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest)

            val req = call.receive<CalendarEventWriteRequest>()
            val updated = CalendarService.updateEvent(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound)

            call.respond(updated)
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest)

            if (CalendarService.deleteEvent(id))
                call.respond(HttpStatusCode.NoContent)
            else
                call.respond(HttpStatusCode.NotFound)
        }
    }
}
