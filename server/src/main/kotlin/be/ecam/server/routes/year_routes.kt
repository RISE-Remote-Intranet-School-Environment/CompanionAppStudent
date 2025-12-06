package be.ecam.server.routes

import be.ecam.server.models.YearWriteRequest
import be.ecam.server.services.YearService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.yearRoutes() {

    route("/years") {

        // 🔹 GET /api/years
        get {
            call.respond(YearService.getAllYears())
        }

        // 🔹 GET /api/years/{id}  (id DB)
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val year = YearService.getYearById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Year not found")

            call.respond(year)
        }

        // 🔹 GET /api/years/by-code/{yearId}  (id logique "Y1")
        get("by-code/{yearId}") {
            val yearId = call.parameters["yearId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "yearId missing")

            call.respond(YearService.getYearByYearId(yearId))
        }

        // 🔹 POST /api/years
        post {
            val req = call.receive<YearWriteRequest>()
            val created = YearService.createYear(req)
            call.respond(HttpStatusCode.Created, created)
        }

        // 🔹 PUT /api/years/{id}
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<YearWriteRequest>()
            val updated = YearService.updateYear(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Year not found")

            call.respond(updated)
        }

        // 🔹 DELETE /api/years/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = YearService.deleteYear(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Year not found")
        }
    }
}
