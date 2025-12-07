package be.ecam.server.routes

import be.ecam.server.models.SousCourseWriteRequest
import be.ecam.server.services.SousCourseService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.sousCourseRoutes() {

    route("/sous-courses") {


        get {
            call.respond(SousCourseService.getAllSousCourses())
        }


        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val sc = SousCourseService.getSousCourseById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Sous-course not found")

            call.respond(sc)
        }


        get("by-sous-id/{sousCourseId}") {
            val sousId = call.parameters["sousCourseId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "sousCourseId missing")

            call.respond(SousCourseService.getSousCoursesBySousCourseId(sousId))
        }

        get("by-course/{courseId}") {
            val courseId = call.parameters["courseId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "courseId missing")

            call.respond(SousCourseService.getSousCoursesByCourseId(courseId))
        }

        post {
            val req = call.receive<SousCourseWriteRequest>()
            val created = SousCourseService.createSousCourse(req)
            call.respond(HttpStatusCode.Created, created)
        }


        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<SousCourseWriteRequest>()
            val updated = SousCourseService.updateSousCourse(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Sous-course not found")

            call.respond(updated)
        }


        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = SousCourseService.deleteSousCourse(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Sous-course not found")
        }
    }
}
