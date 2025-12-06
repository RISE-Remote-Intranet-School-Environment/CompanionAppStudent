package be.ecam.server.routes

import be.ecam.server.models.YearOptionWriteRequest
import be.ecam.server.services.YearOptionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.yearOptionRoutes() {

    route("/year-options") {

        // 🔹 GET /api/year-options
        get {
            call.respond(YearOptionService.getAllYearOptions())
        }

        // 🔹 GET /api/year-options/{id}  (id DB)
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val opt = YearOptionService.getYearOptionById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Year option not found")

            call.respond(opt)
        }

        // 🔹 GET /api/year-options/by-code/{yearOptionId}
        get("by-code/{yearOptionId}") {
            val yearOptionId = call.parameters["yearOptionId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "yearOptionId missing")

            call.respond(YearOptionService.getYearOptionsByYearOptionId(yearOptionId))
        }

        // 🔹 GET /api/year-options/by-bloc/{blocId}
        get("by-bloc/{blocId}") {
            val blocId = call.parameters["blocId"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid blocId")

            call.respond(YearOptionService.getYearOptionsByBlocId(blocId))
        }

        // 🔹 POST /api/year-options
        post {
            val req = call.receive<YearOptionWriteRequest>()
            val created = YearOptionService.createYearOption(req)
            call.respond(HttpStatusCode.Created, created)
        }

        // 🔹 PUT /api/year-options/{id}
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<YearOptionWriteRequest>()
            val updated = YearOptionService.updateYearOption(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Year option not found")

            call.respond(updated)
        }

        // 🔹 DELETE /api/year-options/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = YearOptionService.deleteYearOption(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Year option not found")
        }
    