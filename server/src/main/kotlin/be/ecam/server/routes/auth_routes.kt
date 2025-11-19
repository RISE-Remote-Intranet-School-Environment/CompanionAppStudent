package be.ecam.server.routes

import be.ecam.server.models.AuthResponse
import be.ecam.server.models.AuthUserDTO
import be.ecam.server.models.LoginRequest
import be.ecam.server.models.RegisterRequest
import be.ecam.server.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {

    // POST /api/auth/register
    post("/auth/register") {
        // Parse JSON
        val body = runCatching { call.receive<RegisterRequest>() }.getOrElse {
            call.respond(HttpStatusCode.BadRequest, "JSON invalide")
            return@post
        }

        try {
            val user: AuthUserDTO = AuthService.register(
                RegisterRequest(
                    username = body.username.trim(),
                    email = body.email.trim(),
                    password = body.password
                )
            )
            call.respond(
                HttpStatusCode.Created,
                AuthResponse(
                    user = user,
                    message = "Compte créé"
                )
            )
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.Conflict, e.message ?: "Conflit")
        }
    }

    // POST /api/auth/login
    post("/auth/login") {
        val body = runCatching { call.receive<LoginRequest>() }.getOrElse {
            call.respond(HttpStatusCode.BadRequest, "JSON invalide")
            return@post
        }

        try {
            val user: AuthUserDTO = AuthService.login(
                LoginRequest(
                    emailOrUsername = body.emailOrUsername.trim(),
                    password = body.password
                )
            )
            call.respond(
                AuthResponse(
                    user = user,
                    message = "Connexion OK"
                )
            )
        } catch (e: IllegalArgumentException) {
            // mdp incorrect
            call.respond(HttpStatusCode.Unauthorized, e.message ?: "Identifiants invalides")
        } catch (e: IllegalStateException) {
            // Utilisateur introuvable
            call.respond(HttpStatusCode.Unauthorized, e.message ?: "Identifiants invalides")
        }
    }
}
