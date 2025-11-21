package be.ecam.server.routes

import be.ecam.server.models.CalendarEvent
import be.ecam.server.models.Course
import be.ecam.server.models.CourseTable
import be.ecam.server.services.CalendarService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.calendarRoutes() {

    // GET /api/calendar/events
    get("/calendar/events") {
        val events = CalendarService.getAllEvents()
        call.respond(events)
    }

    // GET /api/calendar/group/{groupCode}
    get("/calendar/group/{groupCode}") {
        val groupCode = call.parameters["groupCode"]
        if (groupCode == null) {
            call.respond(HttpStatusCode.BadRequest, "groupCode manquant")
            return@get
        }

        val events = CalendarService.getEventsForGroup(groupCode)
        call.respond(events)
    }

    // GET /api/calendar/owner/{ownerRef}
    get("/calendar/owner/{ownerRef}") {
        val ownerRef = call.parameters["ownerRef"]
        if (ownerRef == null) {
            call.respond(HttpStatusCode.BadRequest, "ownerRef manquant")
            return@get
        }

        val events = CalendarService.getEventsForOwner(ownerRef)
        call.respond(events)
    }

    // ---------- DEBUG : créer un event de test dans la DB ----------
    // GET /api/calendar/debug/seed-one
    get("/calendar/debug/seed-one") {
        transaction {
            // on prend un cours existant 
            val course = Course.find { CourseTable.code eq "1bach10" }.firstOrNull()

            CalendarEvent.new {
                code = "EO2L-L2-2BA-A"
                title = "Laboratoire d’électronique"
                date = "2025-11-28"
                startTime = "12:45"
                endTime = "16:15"
                room = "1F04"
                sessionNumber = 2
                groupCode = "2BA-s3"
                ownerType = "TEACHER"   
                ownerRef = "DLH"
                this.course = course
            }
        }

        call.respondText("Event de test créé")
    }
}
