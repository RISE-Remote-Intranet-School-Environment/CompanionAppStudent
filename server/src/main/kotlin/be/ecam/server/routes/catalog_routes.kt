package be.ecam.server.routes

import be.ecam.server.services.CatalogService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.catalogRoutes() {

    // GET /api/formations
    get("/formations") {
        val formations = CatalogService.getAllFormations()
        call.respond(formations)
    }

    // GET /api/formations/{slug}/courses
    get("/formations/{slug}/courses") {
        val slug = call.parameters["slug"]
        if (slug == null) {
            call.respond(HttpStatusCode.BadRequest, "Slug manquant")
            return@get
        }

        val courses = CatalogService.getCoursesByFormationSlug(slug)
        call.respond(courses)
    }

    // DEBUG : importer les données depuis le JSON
    get("/debug/seed/formations") {
        try {
            CatalogService.seedFormationsFromJson()
            call.respondText("Formations + cours importés depuis ecam_formations_2025.json")
        } catch (e: Throwable) {
            e.printStackTrace() // log complet côté serveur
            call.respond(
                HttpStatusCode.InternalServerError,
                "Erreur dans seedFormationsFromJson: ${e.message}"
                )
            }
        }
}