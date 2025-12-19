package be.ecam.server.routes

import be.ecam.server.models.AddCourseRequest
import be.ecam.server.models.AddCoursesRequest
import be.ecam.server.services.UserCoursesService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userCoursesRoutes() {
    route("/my-courses") {

        get {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Token invalide")

            val courses = UserCoursesService.getCoursesForUser(userId)
            call.respond(mapOf("courses" to courses))
        }

        post {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Token invalide")

            val body = runCatching { call.receive<AddCourseRequest>() }.getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, "JSON invalide")
            }

            val added = UserCoursesService.addCourse(userId, body.courseId)
            if (added) {
                call.respond(HttpStatusCode.Created, mapOf("message" to "Cours ajouté", "courseId" to body.courseId))
            } else {
                call.respond(HttpStatusCode.Conflict, mapOf("message" to "Cours déjà ajouté"))
            }
        }

        post("batch") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, "Token invalide")

            val body = runCatching { call.receive<AddCoursesRequest>() }.getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, "JSON invalide")
            }

            val added = UserCoursesService.addCourses(userId, body.courseIds)
            call.respond(HttpStatusCode.Created, mapOf("message" to "$added cours ajoutés"))
        }

        put {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, "Token invalide")

            val body = runCatching { call.receive<AddCoursesRequest>() }.getOrElse {
                return@put call.respond(HttpStatusCode.BadRequest, "JSON invalide")
            }

            val count = UserCoursesService.setCourses(userId, body.courseIds)
            call.respond(mapOf("message" to "$count cours définis"))
        }

        delete("{courseId}") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Token invalide")

            val courseId = call.parameters["courseId"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "courseId requis")

            val removed = UserCoursesService.removeCourse(userId, courseId)
            if (removed) {
                call.respond(mapOf("message" to "Cours supprimé"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("message" to "Cours non trouvé"))
            }
        }

        delete {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("id")?.asInt()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Token invalide")

            val count = UserCoursesService.clearCourses(userId)
            call.respond(mapOf("message" to "$count cours supprimés"))
        }
    }
}