package be.ecam.server.routes

// === ROUTES for authentication (register/login) of Admin users ===
import be.ecam.server.models.* // import DTOs
import be.ecam.server.services.AuthService
import io.ktor.http.* // for HttpStatusCode
import io.ktor.server.application.* // for ApplicationCall
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    // instantiate the AuthService
    val auth = AuthService()

    // Register route
    post("/auth/register") {
        val body = runCatching { call.receive<RegisterRequest>() }.getOrElse {
            call.respond(HttpStatusCode.BadRequest, "JSON invalide"); return@post
        }
        try {
            val a = auth.register(body.username.trim(), body.email.trim(), body.password)
            call.respond(HttpStatusCode.Created, AuthResponse(AuthUserDTO(a.id.value, a.username, a.email), "Compte créé"))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.Conflict, e.message ?: "Conflit")
        }
    }

    // Login route
    post("/auth/login") {
        val body = runCatching { call.receive<LoginRequest>() }.getOrElse {
            call.respond(HttpStatusCode.BadRequest, "JSON invalide"); return@post
        }
        val a = auth.login(body.emailOrUsername.trim(), body.password)
        if (a == null) call.respond(HttpStatusCode.Unauthorized, "Identifiants invalides")
        else call.respond(AuthResponse(AuthUserDTO(a.id.value, a.username, a.email), "Connexion OK"))
    }
}

