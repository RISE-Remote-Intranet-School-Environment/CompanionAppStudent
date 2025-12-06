package be.ecam.server.routes

import be.ecam.server.models.ProfessorWriteRequest
import be.ecam.server.services.ProfessorService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.professorRoutes() {

    route("/professors") {

        // 🔹 GET /api/professors
        get {
            call.respond(ProfessorService.getAllProfessors())
        }

        // 🔹 GET /api/professors/{id}
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val prof = ProfessorService.getProfessorById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Professor not found")

            call.respond(prof)
        }

        // 🔹 GET /api/professors/by-professor-id/{professorId}
        get("by-professor-id/{professorId}") {
            val professorId = call.parameters["professorId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "professorId missing")

            val prof = ProfessorService.getProfessorByProfessorId(professorId)
            if (prof == null) call.respond(HttpStatusCode.NotFound, "Professor not found")
            else call.respond(prof)
        }

        // 🔹 GET /api/professors/by-email/{email}
        get("by-email/{email}") {
            val email = call.parameters["email"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "email missing")

            val prof = ProfessorService.getProfessorByEmail(email)
            if (prof == null) call.respond(HttpStatusCode.NotFound, "Professor not found")
            else call.respond(prof)
        }

        // 🔹 GET /api/professors/by-speciality/{speciality}
        get("by-speciality/{speciality}") {
            val speciality = call.parameters["speciality"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "speciality missing")

            call.respond(ProfessorService.getProfessorsBySpeciality(speciality))
        }

        // 🔹 POST /api/professors
        post {
            val req = call.receive<ProfessorWriteRequest>()
            val created = ProfessorService.createProfessor(req)
            call.respond(HttpStatusCode.Created, created)
        }

        // 🔹 PUT /api/professors/{id}
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<ProfessorWriteRequest>()
            val updated = ProfessorService.updateProfessor(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Professor not found")

            call.respond(updated)
        }

        // 🔹 DELETE /api/professors/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = ProfessorService.deleteProfessor(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Professor not found")
        }
    }
}
