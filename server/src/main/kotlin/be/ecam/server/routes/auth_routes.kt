package be.ecam.server.routes

import be.ecam.server.models.*
import be.ecam.server.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    val auth = AuthService()

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

    post("/auth/login") {
        val body = runCatching { call.receive<LoginRequest>() }.getOrElse {
            call.respond(HttpStatusCode.BadRequest, "JSON invalide"); return@post
        }
        val a = auth.login(body.emailOrUsername.trim(), body.password)
        if (a == null) call.respond(HttpStatusCode.Unauthorized, "Identifiants invalides")
        else call.respond(AuthResponse(AuthUserDTO(a.id.value, a.username, a.email), "Connexion OK"))
    }
}

