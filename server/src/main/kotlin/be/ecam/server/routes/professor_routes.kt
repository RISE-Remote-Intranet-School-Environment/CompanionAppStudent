package be.ecam.server.routes

import be.ecam.server.models.ProfessorWriteRequest
import be.ecam.server.services.ProfessorService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.professorRoutes() {

    // get all professors
    get("/professors") {
        call.respond(ProfessorService.getAllProfessors())
    }

    // get professor by id
    // GET by id
    get("/professors/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "IInvalid ID")

        val prof = ProfessorService.getProfessorById(id)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Professor not found")

        call.respond(prof)
    }

    // GET by email
    get("/professors/email/{email}") {
        val email = call.parameters["email"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing email")

        val prof = ProfessorService.getProfessorByEmail(email)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Professeur introuvable")

        call.respond(prof)
    }

    // get professors by speciality
    get("/professors/speciality/{spec}") {
        val speciality = call.parameters["spec"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing speciality")

        val list = ProfessorService.getProfessorsBySpeciality(speciality)
        call.respond(list)
    }
    // create professor
    post("/professors") {
        val req = call.receive<ProfessorWriteRequest>()
        val created = ProfessorService.createProfessor(req)
        call.respond(HttpStatusCode.Created, created)
    }
    // update professor
    put("/professors/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@put call.respond(HttpStatusCode.BadRequest, "IInvalid ID")

        val req = call.receive<ProfessorWriteRequest>()
        val updated = ProfessorService.updateProfessor(id, req)
            ?: return@put call.respond(HttpStatusCode.NotFound, "Prof not found")

        call.respond(updated)
    }

    // delete professor
    delete("/professors/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "IInvalid ID")

        val ok = ProfessorService.deleteProfessor(id)
        if (ok) call.respond(HttpStatusCode.NoContent)
        else call.respond(HttpStatusCode.NotFound, "Prof not found")
    }

    // debug route to seed professors from JSON file
    get("/debug/seed/professors") {
        try {
            ProfessorService.seedProfessorsFromJson()
            call.respondText("Professors imported from ecam_professors_2025.json")
        } catch (e: Throwable) {
            call.respond(
                HttpStatusCode.InternalServerError,
                "Error during import: ${e.message}"
            )
        }
    }
}
