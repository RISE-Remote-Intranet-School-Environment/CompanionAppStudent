package be.ecam.server.routes

import be.ecam.server.models.CourseEvaluationWriteRequest
import be.ecam.server.services.CourseEvaluationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courseEvaluationRoutes() {

    route("/course-evaluations") {

        
        get {
            call.respond(CourseEvaluationService.getAllEvaluations())
        }

        )
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val eval = CourseEvaluationService.getEvaluationById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Course evaluation not found")

            call.respond(eval)
        }

        
        get("by-activity/{activityId}") {
            val activityId = call.parameters["activityId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "activityId missing")

            call.respond(CourseEvaluationService.getEvaluationsByActivityId(activityId))
        }

        
        get("by-course/{courseId}") {
            val courseId = call.parameters["courseId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "courseId missing")

            call.respond(CourseEvaluationService.getEvaluationsByCourseId(courseId))
        }

        
        post {
            val req = call.receive<CourseEvaluationWriteRequest>()
            val created = CourseEvaluationService.createEvaluation(req)
            call.respond(HttpStatusCode.Created, created)
        }

        
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<CourseEvaluationWriteRequest>()
            val updated = CourseEvaluationService.updateEvaluation(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Course evaluation not found")

            call.respond(updated)
        }

        
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = CourseEvaluationService.deleteEvaluation(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Course evaluation not found")
        }
    }
}
