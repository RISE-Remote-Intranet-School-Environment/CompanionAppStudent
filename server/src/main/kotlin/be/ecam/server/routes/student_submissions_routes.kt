package be.ecam.server.routes

import be.ecam.server.models.StudentSubmissionCreateRequest
import be.ecam.server.models.StudentSubmissionUpdateRequest
import be.ecam.server.services.StudentSubmissionsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentSubmissionsRoutes() {

    route("/student-submissions") {

        post {
            val body = call.receive<StudentSubmissionCreateRequest>()
            val created = StudentSubmissionsService.createSubmission(body)
            call.respond(HttpStatusCode.Created, created)
        }

        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid submission id")

            val submission = StudentSubmissionsService.getSubmissionById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Submission not found")

            call.respond(submission)
        }

        patch("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, "Invalid submission id")

            val body = call.receive<StudentSubmissionUpdateRequest>()
            val updated = StudentSubmissionsService.updateSubmission(id, body)
                ?: return@patch call.respond(HttpStatusCode.NotFound, "Submission not found")

            call.respond(updated)
        }

        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid submission id")

            val deleted = StudentSubmissionsService.deleteSubmission(id)
            if (deleted) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Submission not found")
        }
    }

    get("/students/{studentId}/submissions") {
        val studentId = call.parameters["studentId"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid student id")

        call.respond(StudentSubmissionsService.getSubmissionsByStudent(studentId))
    }

    get("/courses/{courseId}/submissions") {
        val courseId = call.parameters["courseId"]?.takeIf { it.isNotBlank() }
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid course id")

        call.respond(StudentSubmissionsService.getSubmissionsByCourse(courseId))
    }

    get("/sous-courses/{sousCourseId}/submissions") {
        val sousCourseId = call.parameters["sousCourseId"]?.takeIf { it.isNotBlank() }
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid sous-course id")

        call.respond(StudentSubmissionsService.getSubmissionsBySousCourse(sousCourseId))
    }
}
