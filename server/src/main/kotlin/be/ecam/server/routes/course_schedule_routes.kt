package be.ecam.server.routes

import be.ecam.server.models.CourseScheduleWriteRequest
import be.ecam.server.services.CourseScheduleService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courseScheduleRoutes() {

    route("/course-schedule") {

        
        get {
            call.respond(CourseScheduleService.getAllSchedules())
        }

        
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val schedule = CourseScheduleService.getScheduleById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Schedule not found")

            call.respond(schedule)
        }

        
        get("by-week/{week}") {
            val week = call.parameters["week"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid week")

            call.respond(CourseScheduleService.getSchedulesByWeek(week))
        }

        
        get("by-date/{date}") {
            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "date missing")

            call.respond(CourseScheduleService.getSchedulesByDate(date))
        }

        
        get("by-year-option/{yearOptionId}") {
            val yo = call.parameters["yearOptionId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "yearOptionId missing")

            call.respond(CourseScheduleService.getSchedulesByYearOption(yo))
        }

        
        get("by-group/{groupNo}") {
            val group = call.parameters["groupNo"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "groupNo missing")

            call.respond(CourseScheduleService.getSchedulesByGroup(group))
        }

        
        
        get("by-raccourci/{shortId}") {
            val shortId = call.parameters["shortId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "shortId missing")

            call.respond(CourseScheduleService.getSchedulesByRaccourci(shortId))
        }

        
        post {
            val req = call.receive<CourseScheduleWriteRequest>()
            val created = CourseScheduleService.createSchedule(req)
            call.respond(HttpStatusCode.Created, created)
        }

        
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<CourseScheduleWriteRequest>()
            val updated = CourseScheduleService.updateSchedule(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Schedule not found")

            call.respond(updated)
        }

        
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = CourseScheduleService.deleteSchedule(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Schedule not found")
        }
    }
}
