package be.ecam.server.routes

import be.ecam.server.models.BlockWriteRequest
import be.ecam.server.models.CourseWriteRequest
import be.ecam.server.models.FormationWriteRequest
import be.ecam.server.services.CatalogService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.catalogRoutes() {

    // GET /api/formations
    get("/formations") {
        val formations = CatalogService.getAllFormations()
        call.respond(formations)
    }

    // GET /api/formations/{slug}
    // -> on cherche la formation dans la liste renvoyée par getAllFormations()
    get("/formations/{slug}") {
        val slug = call.parameters["slug"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Slug manquant")

        val formation = CatalogService
            .getAllFormations()
            .find { it.slug == slug }

        if (formation == null) {
            call.respond(HttpStatusCode.NotFound, "Formation non trouvée")
        } else {
            call.respond(formation)
        }
    }

    // GET /api/formations/{slug}/courses
    get("/formations/{slug}/courses") {
        val slug = call.parameters["slug"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Slug manquant")

        val courses = CatalogService.getCoursesByFormationSlug(slug)
        call.respond(courses)
    }

    // GET /api/courses/code/{code}/details
    get("/courses/code/{code}/details") {
        val code = call.parameters["code"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Code manquant")

        val details = CatalogService.getCourseDetailsByCode(code)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Détails introuvables")

        call.respond(details)
    }


    // GET /api/courses/{id}
    // -> on balaye tous les cours de toutes les formations
    get("/courses/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "ID invalide")

        val formations = CatalogService.getAllFormations()
        val slugs = formations.map { it.slug }

        val course = slugs
            .asSequence()
            .flatMap { slug ->
                CatalogService.getCoursesByFormationSlug(slug).asSequence()
            }
            .find { it.id == id }

        if (course == null) {
            call.respond(HttpStatusCode.NotFound, "Cours introuvable")
        } else {
            call.respond(course)
        }
    }

    // GET /api/courses/code/{code}
    get("/courses/code/{code}") {
        val code = call.parameters["code"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "Code manquant")

        val formations = CatalogService.getAllFormations()
        val slugs = formations.map { it.slug }

        val course = slugs
            .asSequence()
            .flatMap { slug ->
                CatalogService.getCoursesByFormationSlug(slug).asSequence()
            }
            .find { it.code.equals(code, ignoreCase = true) }

        if (course == null) {
            call.respond(HttpStatusCode.NotFound, "Cours introuvable")
        } else {
            call.respond(course)
        }
    }


    // GET /api/courses/{id}/details
    // Récupère les détails d'un cours à partir de son ID (courses.id)
    get("/courses/{id}/details") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "ID invalide")

        val details = CatalogService.getCourseDetailsByCourseId(id)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Détails introuvables")

        call.respond(details)
    }



    // debug route to seed formations from JSON
    get("/debug/seed/formations") {
        try {
            CatalogService.seedFormationsFromJson()
            call.respondText("Formations + cours importés depuis ecam_formations_2025.json")
        } catch (e: Throwable) {
            e.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                "Erreur dans seedFormationsFromJson: ${e.message}"
            )
        }
    }


    // crud admin routes
    // --- FORMATIONS CRUD ---

    // GET /api/formations/id/{id}
    get("/formations/id/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "ID invalide")

        val formation = CatalogService.getFormationById(id)
            ?: return@get call.respond(HttpStatusCode.NotFound, "Formation non trouvée")

        call.respond(formation)
    }

    // POST /api/formations
    post("/formations") {
        val req = call.receive<FormationWriteRequest>()
        val formation = CatalogService.createFormation(req)
        call.respond(HttpStatusCode.Created, formation)
    }

    // PUT /api/formations/{id}
    put("/formations/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@put call.respond(HttpStatusCode.BadRequest, "ID invalide")

        val req = call.receive<FormationWriteRequest>()
        val updated = CatalogService.updateFormation(id, req)
            ?: return@put call.respond(HttpStatusCode.NotFound, "Formation non trouvée")

        call.respond(updated)
    }

    // DELETE /api/formations/{id}
    delete("/formations/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "ID invalide")

        val ok = CatalogService.deleteFormation(id)
        if (ok) call.respond(HttpStatusCode.NoContent)
        else call.respond(HttpStatusCode.NotFound, "Formation non trouvée")
    }

    // --- BLOCKS CRUD ---

    // POST /api/blocks
    post("/blocks") {
        try {
            val req = call.receive<BlockWriteRequest>()
            val block = CatalogService.createBlock(req)
            call.respond(HttpStatusCode.Created, block)
        } catch (e: IllegalStateException) {
            // par ex. "Formation 999 introuvable"
            call.respond(
                HttpStatusCode.BadRequest,
                e.message ?: "Données invalides pour le block"
            )
        }
    }

    // PUT /api/blocks/{id}
    put("/blocks/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@put call.respond(HttpStatusCode.BadRequest, "ID invalide")

        try {
            val req = call.receive<BlockWriteRequest>()
            val updated = CatalogService.updateBlock(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Block introuvable")

            call.respond(updated)
        } catch (e: IllegalStateException) {
            call.respond(
                HttpStatusCode.BadRequest,
                e.message ?: "Données invalides pour le block"
            )
        }
    }

    // DELETE /api/blocks/{id}
    delete("/blocks/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "ID invalide")

        val ok = CatalogService.deleteBlock(id)
        if (ok) call.respond(HttpStatusCode.NoContent)
        else call.respond(HttpStatusCode.NotFound, "Block introuvable")
    }

    // --- COURSES CRUD ---

    // POST /api/courses
    post("/courses") {
        try {
            val req = call.receive<CourseWriteRequest>()
            val course = CatalogService.createCourse(req)
            call.respond(HttpStatusCode.Created, course)
        } catch (e: IllegalStateException) {
            // ex: "Block 37 introuvable" ou "Formation 4 introuvable"
            call.respond(
                HttpStatusCode.BadRequest,
                e.message ?: "Données invalides pour le cours"
            )
        }
    }

    // PUT /api/courses/{id}
    put("/courses/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@put call.respond(HttpStatusCode.BadRequest, "ID invalide")

        try {
            val req = call.receive<CourseWriteRequest>()
            val updated = CatalogService.updateCourse(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Cours introuvable")

            call.respond(updated)
        } catch (e: IllegalStateException) {
            call.respond(
                HttpStatusCode.BadRequest,
                e.message ?: "Données invalides pour le cours"
            )
        }
    }

    // DELETE /api/courses/{id}
    delete("/courses/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "ID invalide")

        val ok = CatalogService.deleteCourse(id)
        if (ok) call.respond(HttpStatusCode.NoContent)
        else call.respond(HttpStatusCode.NotFound, "Cours introuvable")
    }
}