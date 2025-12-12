package be.ecam.server.routes

import be.ecam.server.models.CourseResourceCreateRequest
import be.ecam.server.models.CourseResourceUpdateRequest
import be.ecam.server.services.CourseResourcesService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.courseResourcesRoutes() {

    route("/course-resources") {

        get {
            call.respond(CourseResourcesService.getAllResources())
        }

        post {
            val body = call.receive<CourseResourceCreateRequest>()
            val created = CourseResourcesService.createResource(body)
            call.respond(HttpStatusCode.Created, created)
        }

        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid resource id")

            val resource = CourseResourcesService.getResourceById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Resource not found")

            call.respond(resource)
        }

        patch("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, "Invalid resource id")

            val body = call.receive<CourseResourceUpdateRequest>()
            val updated = CourseResourcesService.updateResource(id, body)
                ?: return@patch call.respond(HttpStatusCode.NotFound, "Resource not found")

            call.respond(updated)
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid resource id")

            val ok = CourseResourcesService.deleteResource(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Resource not found")
        }
    }

    get("/professors/{professorId}/resources") {
        val professorId = call.parameters["professorId"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid professor id")

        call.respond(CourseResourcesService.getResourcesByProfessor(professorId))
    }

    get("/courses/{courseId}/resources") {
        val courseId = call.parameters["courseId"]?.takeIf { it.isNotBlank() }
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid course id")

        call.respond(CourseResourcesService.getResourcesByCourse(courseId))
    }

    get("/sous-courses/{sousCourseId}/resources") {
        val sousCourseId = call.parameters["sousCourseId"]?.takeIf { it.isNotBlank() }
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid sous-course id")

        call.respond(CourseResourcesService.getResourcesBySousCourse(sousCourseId))
    }
}
