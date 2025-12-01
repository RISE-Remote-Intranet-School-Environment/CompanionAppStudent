package be.ecam.server.routes

import be.ecam.server.models.*
import be.ecam.server.security.JwtService
import be.ecam.server.services.AuthService
import be.ecam.server.services.AdminService
import be.ecam.server.models.AdminDTO
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*



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

            val token = JwtService.generateToken(user)

            call.respond(
                HttpStatusCode.Created,
                AuthResponse(
                    user = user,
                    message = "Compte créé",
                    token = token
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

            val token = JwtService.generateToken(user)

            call.respond(
                AuthResponse(
                    user = user,
                    message = "Connexion OK",
                    token = token
                )
            )
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.Unauthorized, e.message ?: "Identifiants invalides")
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Unauthorized, e.message ?: "Identifiants invalides")
        }
    }

    authenticate("jwt") {
        get("/auth/me") {
            val principal = call.principal<JWTPrincipal>()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Token invalide")

            val userId = principal.payload.getClaim("id").asInt()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "ID manquant dans le token")

            val user = AdminService.getAdminById(userId)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Utilisateur introuvable")

            call.respond(user)
        }

        put("/auth/me") {
            val principal = call.principal<JWTPrincipal>()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, "Token invalide")

            val userId = principal.payload.getClaim("id").asInt()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, "ID manquant")

            val payload = call.receive<UpdateAdminRequest>() // { username, email }

            val updatedUser = AdminService.updateAdmin(
                id = userId,
                req = payload
            ) ?: return@put call.respond(HttpStatusCode.NotFound, "Utilisateur introuvable")

            call.respond(updatedUser)
        }

    }
}

