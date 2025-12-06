package be.ecam.server.routes

import be.ecam.server.models.*
import be.ecam.server.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
// import io.ktor.server.auth.*
// import io.ktor.server.auth.jwt.*  // selon ton système d'auth

fun Route.userRoutes() {

    // ============================
    //        ADMIN ENDPOINTS
    // ============================
    route("/users") {

        // ⚠️ À protéger avec authenticate("admin") { ... } dans configureRouting

        // GET /api/users
        get {
            val users = UserService.getAllUsers()
            call.respond(users)
        }

        // GET /api/users/{id}
        get("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            val user = UserService.getUserById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, "User not found")

            call.respond(user)
        }

        // POST /api/users  (création d’un user par un admin)
        post {
            val body = runCatching { call.receive<UserWriteRequest>() }.getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
            }

            val created = UserService.createUser(body)
            call.respond(HttpStatusCode.Created, created)
        }

        // PATCH /api/users/{id} (update partiel)
        patch("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@patch call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            val body = runCatching { call.receive<UpdateUserRequest>() }.getOrElse {
                return@patch call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
            }

            val updated = UserService.updateUser(id, body)
                ?: return@patch call.respond(HttpStatusCode.NotFound, "User not found")

            call.respond(updated)
        }

        // DELETE /api/users/{id}
        delete("{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            val ok = UserService.deleteUser(id)
            if (!ok) {
                call.respond(HttpStatusCode.NotFound, "User not found")
            } else {
                call.respond(HttpStatusCode.OK, "User deleted")
            }
        }

        // POST /api/users/{id}/attach-student
        post("{id}/attach-student") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            val body = runCatching { call.receive<AttachStudentRequest>() }.getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
            }

            val updated = UserService.attachStudent(id, body.studentId)
                ?: return@post call.respond(HttpStatusCode.NotFound, "User or student not found")

            call.respond(updated)
        }

        // POST /api/users/{id}/attach-professor
        post("{id}/attach-professor") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            val body = runCatching { call.receive<AttachProfessorRequest>() }.getOrElse {
                return@post call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
            }

            val updated = UserService.attachProfessor(id, body.professorId)
                ?: return@post call.respond(HttpStatusCode.NotFound, "User or professor not found")

            call.respond(updated)
        }
    }

    // ============================
    //      STUDENT – SELF UPDATE
    // ============================
    // Exemple : l'étudiant connecté peut mettre à jour uniquement son avatar (image)
    //
    // Tu mettras cette route dans un bloc `authenticate { ... }` et tu récupéreras
    // l’ID du user courant via ton principal (JWT, session, etc.)
    //
    // Exemple pseudo-code pour récupérer l’ID utilisateur :
    // val principal = call.principal<JWTPrincipal>()
    // val userId = principal!!.getClaim("userId", Int::class)
    //
    post("/users/me/avatar") {
        // TODO : adapter à ton système d'auth.
        // Ici je mets un placeholder pour userId.
        // Remplace par ton propre mécanisme (JWT, session, etc.)
        val userIdHeader = call.request.headers["X-User-Id"]
        val userId = userIdHeader?.toIntOrNull()
            ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing or invalid user id (adapt this to your auth)")

        val body = runCatching { call.receive<UpdateAvatarRequest>() }.getOrElse {
            return@post call.respond(HttpStatusCode.BadRequest, "Invalid JSON")
        }

        val updated = UserService.updateAvatar(userId, body.avatarUrl)
            ?: return@post call.respond(HttpStatusCode.NotFound, "User not found")

        call.respond(updated)
    }
}
