package be.ecam.server.routes

import be.ecam.server.models.PaeStudentWriteRequest
import be.ecam.server.services.PaeStudentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.paeStudentRoutes() {

    route("/pae-students") {

        // 🔹 GET /api/pae-students
        get {
            call.respond(PaeStudentService.getAllPaeStudents())
        }

        // 🔹 GET /api/pae-students/{id}  (id DB)
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val student = PaeStudentService.getPaeStudentById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "PAE student not found")

            call.respond(student)
        }

        // 🔹 GET /api/pae-students/by-student-id/{studentId}
        get("by-student-id/{studentId}") {
            val studentId = call.parameters["studentId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid studentId")

            call.respond(PaeStudentService.getPaeStudentsByStudentId(studentId))
        }

        // 🔹 GET /api/pae-students/by-formation/{formationId}
        get("by-formation/{formationId}") {
            val formationId = call.parameters["formationId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "formationId missing")

            call.respond(PaeStudentService.getPaeStudentsByFormation(formationId))
        }

        // 🔹 GET /api/pae-students/by-bloc/{blocId}
        get("by-bloc/{blocId}") {
            val blocId = call.parameters["blocId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "blocId missing")

            call.respond(PaeStudentService.getPaeStudentsByBloc(blocId))
        }

        // 🔹 POST /api/pae-students
        post {
            val req = call.receive<PaeStudentWriteRequest>()
            val created = PaeStudentService.createPaeStudent(req)
            call.respond(HttpStatusCode.Created, created)
        }

        // 🔹 PUT /api/pae-students/{id}
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<PaeStudentWriteRequest>()
            val updated = PaeStudentService.updatePaeStudent(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "PAE student not found")

            call.respond(updated)
        }

        // 🔹 DELETE /api/pae-students/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = PaeStudentService.deletePaeStudent(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "PAE student not found")
        }
    }
}
