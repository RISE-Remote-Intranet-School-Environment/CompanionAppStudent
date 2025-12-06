package be.ecam.server.routes

import be.ecam.server.models.CourseWriteRequest
import be.ecam.server.services.CourseService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courseRoutes() {

    route("/courses") {

        // 🔹 GET /api/courses
        get {
            call.respond(CourseService.getAllCourses())
        }

        // 🔹 GET /api/courses/{id}  (id DB)
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val course = CourseService.getCourseById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Course not found")

            call.respond(course)
        }

        // 🔹 GET /api/courses/by-code/{courseId}  (id logique "4EIDB40")
        get("by-code/{courseId}") {
            val courseId = call.parameters["courseId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "courseId missing")

            call.respond(CourseService.getCoursesByCourseId(courseId))
        }

        // 🔹 GET /api/courses/by-short-id/{shortId}  (ex: "DB")
        get("by-short-id/{shortId}") {
            val shortId = call.parameters["shortId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "shortId missing")

            call.respond(CourseService.getCoursesByShortId(shortId))
        }

        // 🔹 GET /api/courses/by-bloc/{blocId}
        get("by-bloc/{blocId}") {
            val blocId = call.parameters["blocId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid blocId")

            call.respond(CourseService.getCoursesByBlocId(blocId))
        }

        // 🔹 GET /api/courses/by-formation/{formationId}
        get("by-formation/{formationId}") {
            val formationId = call.parameters["formationId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid formationId")

            call.respond(CourseService.getCoursesByFormationId(formationId))
        }

        // 🔹 POST /api/courses
        post {
            val req = call.receive<CourseWriteRequest>()
            val created = CourseService.createCourse(req)
            call.respond(HttpStatusCode.Created, created)
        }

        // 🔹 PUT /api/courses/{id}
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<CourseWriteRequest>()
            val updated = CourseService.updateCourse(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Course not found")

            call.respond(updated)
        }

        // 🔹 DELETE /api/courses/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = CourseService.deleteCourse(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Course not found")
        }
    }
}
