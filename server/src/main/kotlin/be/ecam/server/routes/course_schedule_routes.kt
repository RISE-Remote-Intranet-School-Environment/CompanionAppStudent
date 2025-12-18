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

        // GET /api/course-schedule?yearOptionId=...&seriesId=...&startDate=...&endDate=...
        get {
            val yearOptionId = call.request.queryParameters["yearOptionId"]
            val seriesId = call.request.queryParameters["seriesId"]
            val startDate = call.request.queryParameters["startDate"]
            val endDate = call.request.queryParameters["endDate"]

            val schedules = CourseScheduleService.getAllFiltered(
                yearOptionId = yearOptionId,
                seriesId = seriesId,
                startDate = startDate,
                endDate = endDate
            )
            call.respond(schedules)
        }

        // GET /api/course-schedule/{id}
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            val schedule = CourseScheduleService.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Schedule not found")

            call.respond(schedule)
        }

        // GET /api/course-schedule/by-date/{date}
        get("by-date/{date}") {
            val date = call.parameters["date"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Date required")

            val schedules = CourseScheduleService.getByDate(date)
            call.respond(schedules)
        }

        // GET /api/course-schedule/by-year-option/{yearOptionId}
        get("by-year-option/{yearOptionId}") {
            val yearOptionId = call.parameters["yearOptionId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "YearOptionId required")

            val schedules = CourseScheduleService.getByYearOption(yearOptionId)
            call.respond(schedules)
        }

        // GET /api/course-schedule/by-course/{courseId}
        get("by-course/{courseId}") {
            val courseId = call.parameters["courseId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "CourseId required")

            val schedules = CourseScheduleService.getByCourse(courseId)
            call.respond(schedules)
        }

        // POST /api/course-schedule
        post {
            val body = runCatching { call.receive<CourseScheduleWriteRequest>() }.getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
            }

            val created = CourseScheduleService.create(body)
            call.respond(HttpStatusCode.Created, created)
        }

        // DELETE /api/course-schedule/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            val deleted = CourseScheduleService.delete(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, "Deleted")
            } else {
                call.respond(HttpStatusCode.NotFound, "Not found")
            }
        }
    }
}
