package be.ecam.server.routes

import be.ecam.server.models.CalendarEventWriteRequest
import be.ecam.server.services.CalendarService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.calendarRoutes() {

    // -------- GET EVENTS --------

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

    // -------- CRUD EVENTS (admin) --------

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

    // ============================================================
    //           COURSE SCHEDULE (emplois du temps)
    // ============================================================

    // Tous les créneaux
    get("/calendar/schedule") {
        call.respond(CalendarService.getAllCourseSchedule())
    }

    // Par semaine
    get("/calendar/schedule/week/{week}") {
        val week = call.parameters["week"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "week invalide")

        call.respond(CalendarService.getScheduleForWeek(week))
    }

    // Par année / option (ex: "3BA", "4E_EI", etc.)
    get("/calendar/schedule/year/{year}") {
        val year = call.parameters["year"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "year manquant")

        call.respond(CalendarService.getScheduleForYear(year))
    }

    // Par année + groupe (ex: year=3BA, group=2)
    get("/calendar/schedule/year/{year}/group/{group}") {
        val year = call.parameters["year"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "year manquant")
        val group = call.parameters["group"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "group invalide")

        call.respond(CalendarService.getScheduleForYearAndGroup(year, group))
    }

    // Par code de cours (ex: "3bect30")
    get("/calendar/schedule/course/{code}") {
        val code = call.parameters["code"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "code manquant")

        call.respond(CalendarService.getScheduleForCourse(code))
    }

    // -------- DEBUG --------

    get("/calendar/debug/seed-one") {
        call.respondText("Route debug active (pas d’action)")
    }

    // Import events depuis ecam_calendar_events_2025_2026.json
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

    // Import schedule depuis ecam_calendar_courses_schedule_2025.json
    get("/calendar/debug/seed/schedule") {
        try {
            CalendarService.seedCourseScheduleFromJson()
            call.respondText("Course schedule importé depuis ecam_calendar_courses_schedule_2025.json")
        } catch (e: Throwable) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                "Erreur dans seedCourseScheduleFromJson: ${e.message}"
            )
        }
    }
}
