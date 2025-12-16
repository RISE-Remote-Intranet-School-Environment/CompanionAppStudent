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
            val user = AuthService.register(
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
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, e.message ?: "Conflit")
        }
    }


    // login (PUBLIC)
    post("/auth/login") {
        val body = runCatching { call.receive<LoginRequest>() }.getOrElse {
            call.respond(HttpStatusCode.BadRequest, "JSON invalide")
            return@post
        }

        try {
            val user = AuthService.login(
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

