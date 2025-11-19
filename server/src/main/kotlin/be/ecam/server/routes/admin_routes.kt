package be.ecam.server.routes

import be.ecam.server.models.UpdateAdminRequest
import be.ecam.server.services.AdminService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminRoutes() {

    // ------ GET /api/admins ------
    get("/admins") {
        val admins = AdminService.getAllAdmins()
        call.respond(admins)
    }

    // ------ GET /api/admins/{id} ------
    get("/admins/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "Id invalide")
            return@get
        }

        val admin = AdminService.getAdminById(id)
        if (admin == null) {
            call.respond(HttpStatusCode.NotFound, "Admin introuvable")
        } else {
            call.respond(admin)
        }
    }

    // ------ PATCH /api/admins/{id} ------
    patch("/admins/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "ID invalide")
            return@patch
        }

        val body = runCatching { call.receive<UpdateAdminRequest>() }.getOrElse {
            call.respond(HttpStatusCode.BadRequest, "Données invalides")
            return@patch
        }

        val updated = AdminService.updateAdmin(id, body)
        if (updated == null) {
            call.respond(HttpStatusCode.NotFound, "Admin non trouvé")
        } else {
            call.respond(updated)
        }
    }

    // ------ DELETE /api/admins/{id} ------
    delete("/admins/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.BadRequest, "ID invalide")
            return@delete
        }

        val ok = AdminService.deleteAdmin(id)
        if (!ok) {
            call.respond(HttpStatusCode.NotFound, "Admin introuvable")
        } else {
            call.respond(HttpStatusCode.OK, "Admin supprimé")
        }
    }
}
