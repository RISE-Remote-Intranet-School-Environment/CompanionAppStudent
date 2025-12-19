package be.ecam.server.routes

import be.ecam.server.models.CourseScheduleWriteRequest
import be.ecam.server.services.CourseScheduleService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courseScheduleRoutes() {
    route("/course-schedule") {

        // DEBUG: Comptage des entrées
        get("count") {
            val count = CourseScheduleService.getAll().size
            call.respond(mapOf("count" to count))
        }

        // 🔥 NOUVEAU: Mon horaire personnel (basé sur le PAE via JWT)
        get("my-schedule") {
            val principal = call.principal<JWTPrincipal>()
            val email = principal?.payload?.getClaim("email")?.asString()

            if (email.isNullOrBlank()) {
                println("❌ my-schedule: email manquant dans le token JWT")
                return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Email missing in token"))
            }

            println("📅 Fetching personal schedule for: $email")
            val schedules = CourseScheduleService.getScheduleForStudent(email)
            
            println("📅 Found ${schedules.size} events for $email")
            call.respond(schedules)
        }

        // GET /api/course-schedule?yearOptionId=...&seriesId=...&startDate=...&endDate=...
        get {
            // ...existing code...
            val yearOptionId = call.request.queryParameters["yearOptionId"]
            val seriesId = call.request.queryParameters["seriesId"]
            val startDate = call.request.queryParameters["startDate"]
            val endDate = call.request.queryParameters["endDate"]

            println("📅 GET /api/course-schedule yearOptionId=$yearOptionId seriesId=$seriesId")

            val schedules = CourseScheduleService.getAllFiltered(
                yearOptionId = yearOptionId,
                seriesId = seriesId,
                startDate = startDate,
                endDate = endDate
            )
            
            println("📅 Retourne ${schedules.size} cours")
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
