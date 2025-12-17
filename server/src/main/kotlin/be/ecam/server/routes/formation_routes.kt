package be.ecam.server.routes

import be.ecam.server.models.FormationWriteRequest
import be.ecam.server.services.FormationService
import be.ecam.server.services.CourseService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.formationRoutes() {

    route("/formations") {

        
        get {
            call.respond(FormationService.getAllFormations())
        }

        
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val formation = FormationService.getFormationById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Formation not found")

            call.respond(formation)
        }

        
        get("by-code/{formationId}") {
            val formationId = call.parameters["formationId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "formationId missing")

            call.respond(FormationService.getFormationByFormationId(formationId))
        }


        get("/with-courses") {
            val formations = FormationService.getAllFormations()
            val allCourses = CourseService.getAllCourses()
            
            val result = formations.map { formation ->
                mapOf(
                    "formation" to formation,
                    "courses" to allCourses.filter { it.formationId == formation.formationId }
                )
            }
            call.respond(result)
        }

        
        post {
            val req = call.receive<FormationWriteRequest>()
            val created = FormationService.createFormation(req)
            call.respond(HttpStatusCode.Created, created)
        }

        
        put("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val req = call.receive<FormationWriteRequest>()
            val updated = FormationService.updateFormation(id, req)
                ?: return@put call.respond(HttpStatusCode.NotFound, "Formation not found")

            call.respond(updated)
        }

        
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid id")

            val ok = FormationService.deleteFormation(id)
            if (ok) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound, "Formation not found")
        }
    }
}
