package be.ecam.server.routes

import be.ecam.server.models.ProfessorWriteRequest
import be.ecam.server.services.ProfessorService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.professorRoutes() {

    // =============================
    // GET ALL
    // =============================
    get("/professors") {
        call.respond(ProfessorService.getAllProfessors())
    }

    // =============================
    // GET by id
    // =============================
    get("/professors/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "ID invalide")

        val prof = ProfessorService.getProfessorById(id)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Professeur introuvable")

        call.respond(prof)
    }

    // =============================
    // GET by email
    // =============================
    get("/professors/email/{email}") {
        val email = call.parameters["email"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Email manquant")

        val prof = ProfessorService.getProfessorByEmail(email)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Professeur introuvable")

        call.respond(prof)
    }

    // =============================
    // GET by speciality
    // (nouvelle fonction du service)
    // =============================
    get("/professors/speciality/{spec}") {
        val speciality = call.parameters["spec"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Speciality manquante")

        val list = ProfessorService.getProfessorsBySpeciality(speciality)
        call.respond(list)
    }

    // =============================
    // CREATE
    // =============================
    post("/professors") {
        val req = call.receive<ProfessorWriteRequest>()
        val created = ProfessorService.createProfessor(req)
        call.respond(HttpStatusCode.Created, created)
    }

    // =============================
    // UPDATE
    // =============================
    put("/professors/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@put call.respond(HttpStatusCode.BadRequest, "ID invalide")

        val req = call.receive<ProfessorWriteRequest>()
        val updated = ProfessorService.updateProfessor(id, req)
            ?: return@put call.respond(HttpStatusCode.NotFound, "Prof introuvable")

        call.respond(updated)
    }

    // =============================
    // DELETE
    // =============================
    delete("/professors/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "ID invalide")

        val ok = ProfessorService.deleteProfessor(id)
        if (ok) call.respond(HttpStatusCode.NoContent)
        else call.respond(HttpStatusCode.NotFound, "Prof introuvable")
    }

    // =============================
    // DEBUG — Seed JSON
    // =============================
    get("/debug/seed/professors") {
        try {
            ProfessorService.seedProfessorsFromJson()
            call.respondText("Professeurs importés depuis ecam_professors_2025.json")
        } catch (e: Throwable) {
            call.respond(
                HttpStatusCode.InternalServerError,
                "Erreur lors de l'import: ${e.message}"
            )
        }
    }
}
