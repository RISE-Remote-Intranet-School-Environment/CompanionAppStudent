package be.ecam.server.routes

import be.ecam.server.models.CalendarEventWriteRequest
import be.ecam.server.services.CalendarService

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.calendarRoutes() {

    // -------- GET --------

    get("/calendar/events") {
        call.respond(CalendarService.getAllEvents())
    }

    get("/calendar/group/{groupCode}") {
        val groupCode = call.parameters["groupCode"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "groupCode manquant")

        call.respond(CalendarService.getEventsForGroup(groupCode))
    }

    get("/calendar/owner/{ownerRef}") {
        val ownerRef = call.parameters["ownerRef"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "ownerRef manquant")

        call.respond(CalendarService.getEventsForOwner(ownerRef))
    }

    // -------- CRUD (admin) --------

    post("/calendar/events") {
        val req = call.receive<CalendarEventWriteRequest>()
        val event = CalendarService.createEvent(req)
        call.respond(HttpStatusCode.Created, event)
    }

    put("/calendar/events/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@put call.respond(HttpStatusCode.BadRequest, "ID invalide")

        val req = call.receive<CalendarEventWriteRequest>()
        val updated = CalendarService.updateEvent(id, req)
            ?: return@put call.respond(HttpStatusCode.NotFound, "Événement introuvable")

        call.respond(updated)
    }

    delete("/calendar/events/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "ID invalide")

        val ok = CalendarService.deleteEvent(id)
        if (ok) call.respond(HttpStatusCode.NoContent)
        else call.respond(HttpStatusCode.NotFound, "Événement introuvable")
    }

    // -------- DEBUG --------

    get("/calendar/debug/seed-one") {
        call.respondText("Route debug active (pas d’action)")
    }
    
    // route de debug pour importer les events depuis le JSON
    get("/calendar/debug/seed/events") {
        try {
            CalendarService.seedCalendarEventsFromJson()
            call.respondText("Calendar events importés depuis ecam_calendar_events_2025_2026.json")
        } catch (e: Throwable) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                "Erreur dans seedCalendarEventsFromJson: ${e.message}"
            )
        }
    }
}