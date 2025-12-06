package be.ecam.server.routes

import be.ecam.server.models.SeriesNameWriteRequest
import be.ecam.server.services.SeriesNameService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.seriesNameRoutes() {

    route("/series") {

        // 🔹 GET /api/series
        get {
            call.respond(SeriesNameService.getAllSeries())
        }

        // 🔹 GET /api/series/{id}
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val series = SeriesNameService.getSeriesById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Series not found")

            call.respond(series)
        }

        // 🔹 GET /api/series/by-code/{seriesId}
        get("by-code/{seriesId}") {
            val seriesId = call.parameters["seriesId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "seriesId missing")

            call.respond(SeriesNameService.getSeriesBySeriesId(seriesId))
        }

        // 🔹 POST /api/series
        post {
            val req = call.receive<SeriesNameWriteRequest>()
            val created = SeriesNameService.createSeries(req)
            call.respond(HttpStatusCode.Created, created)
        }

        // 🔹 PUT /api/series/{id}
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<SeriesNameWriteRequest>()
            val updated = SeriesNameService.updateSeries(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Series not found")

            call.respond(updated)
        }

        // 🔹 DELETE /api/series/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = SeriesNameService.deleteSeries(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Series not found")
        }
    }
}
