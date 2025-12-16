package be.ecam.server.routes

import be.ecam.server.models.*
import be.ecam.server.security.JwtService
import be.ecam.server.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class UpdateMeRequest(
    val username: String,
    val email: String
)

@Serializable
data class UpdateMeResponse(
    val user: AuthUserDTO,
    val message: String
)

fun Route.authRoutes() {


    // POST /api/auth/register
    post("/auth/register") {
        val body = runCatching { call.receive<RegisterRequest>() }.getOrElse {
            call.respond(HttpStatusCode.BadRequest, "JSON invalide")
            return@post
        }

        try {
            // AuthService.register renvoie maintenant AuthResponse directement
            val response = AuthService.register(body)
            call.respond(HttpStatusCode.Created, response)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.Conflict, e.message ?: "Conflit")
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, e.message ?: "Erreur serveur")
        }
    }

    // POST /api/auth/login
    post("/auth/login") {
        val body = runCatching { call.receive<LoginRequest>() }.getOrElse {
            call.respond(HttpStatusCode.BadRequest, "JSON invalide")
            return@post
        }

        try {
            val response = AuthService.login(body)
            call.respond(response)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.Unauthorized, "Identifiants invalides")
        }
    }

    // POST /api/auth/refresh (NOUVEAU)
    post("/auth/refresh") {
        // On attend { "refreshToken": "..." }
        val body = call.receive<Map<String, String>>()
        val refreshToken = body["refreshToken"]

        if (refreshToken.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Refresh token manquant")
            return@post
        }

        val newAuthResponse = AuthService.refreshToken(refreshToken)

        if (newAuthResponse != null) {
            call.respond(newAuthResponse)
        } else {
            call.respond(HttpStatusCode.Unauthorized, "Refresh token invalide ou expiré")
        }
    }

    // me routes (PROTECTED)
    authenticate("auth-jwt") {

        // GET /api/auth/me
        get("/auth/me") {
            val principal = call.principal<JWTPrincipal>()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Token manquant")

            val id = principal.payload.getClaim("id").asInt()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, "Token invalide")

            try {
                val user = AuthService.getUserById(id)
                call.respond(user) 
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, e.message ?: "User not found")
            }
        }

        // PUT /api/auth/me
        put("/auth/me") {
            val principal = call.principal<JWTPrincipal>()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, "Token manquant")

            val id = principal.payload.getClaim("id").asInt()
                ?: return@put call.respond(HttpStatusCode.Unauthorized, "Token invalide")

            val body = runCatching { call.receive<UpdateMeRequest>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, "JSON invalide")
                return@put
            }

            try {
                val updated = AuthService.updateMe(id, body.username, body.email)
                call.respond(
                    UpdateMeResponse(
                        user = updated,
                        message = "Profil mis à jour"
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Bad request")
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.NotFound, e.message ?: "User not found")
            }
        }
    }
}

