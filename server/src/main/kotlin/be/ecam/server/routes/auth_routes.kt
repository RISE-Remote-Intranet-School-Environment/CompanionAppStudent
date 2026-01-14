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
import io.ktor.util.date.GMTDate

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

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String? = null
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
            
            // Définir le refresh token en cookie HttpOnly
            call.response.cookies.append(
                Cookie(
                    name = "refresh_token",
                    value = response.refreshToken,
                    httpOnly = true,
                    secure = true, // HTTPS uniquement
                    path = "/api/auth",
                    maxAge = 7 * 24 * 60 * 60, // 7 jours
                    extensions = mapOf("SameSite" to "Strict")
                )
            )
            
            // Réponse sans le refresh token (il est dans le cookie)
            call.respond(response.copy(refreshToken = ""))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.Unauthorized, "Identifiants invalides")
        }
    }

    // POST /api/auth/refresh
    post("/auth/refresh") {
        // 1. Essayer le cookie d'abord (Web)
        var refreshToken = call.request.cookies["refresh_token"]
        
        // 2. Si pas de cookie, essayer le body (Mobile/Desktop)
        if (refreshToken.isNullOrBlank()) {
            val body = runCatching { call.receive<RefreshTokenRequest>() }.getOrNull()
            refreshToken = body?.refreshToken
        }

        if (refreshToken.isNullOrBlank()) {
            call.respond(HttpStatusCode.Unauthorized, "Refresh token manquant")
            return@post
        }

        val newAuthResponse = AuthService.refreshToken(refreshToken)

        if (newAuthResponse != null) {
            // Mettre à jour le cookie avec le nouveau refresh token
            call.response.cookies.append(
                Cookie(
                    name = "refresh_token",
                    value = newAuthResponse.refreshToken,
                    httpOnly = true,
                    secure = true,
                    path = "/api/auth",
                    maxAge = 7 * 24 * 60 * 60,
                    extensions = mapOf("SameSite" to "Strict")
                )
            )
            
            // Renvoyer le refresh token dans la réponse pour mobile/desktop
            call.respond(newAuthResponse)
        } else {
            call.response.cookies.append(
                Cookie(
                    name = "refresh_token",
                    value = "",
                    path = "/api/auth",
                    expires = GMTDate.START,
                    httpOnly = true,
                    secure = true
                )
            )
            call.respond(HttpStatusCode.Unauthorized, "Refresh token invalide ou expiré")
        }
    }

    // POST /api/auth/logout
    post("/auth/logout") {
        // 1. Essayer le cookie d'abord (Web)
        var refreshToken = call.request.cookies["refresh_token"]
        
        // 2. Si pas de cookie, essayer le body (Mobile/Desktop)
        if (refreshToken.isNullOrBlank()) {
            val body = runCatching { call.receive<RefreshTokenRequest>() }.getOrNull()
            refreshToken = body?.refreshToken
        }
        
        // 3. Révoquer le token s'il existe
        if (!refreshToken.isNullOrBlank()) {
            AuthService.revokeRefreshToken(refreshToken)
        }
        
        // 4. Invalider le cookie (pour le web)
        call.response.cookies.append(
            Cookie(
                name = "refresh_token",
                value = "",
                path = "/api/auth",
                expires = GMTDate.START,
                httpOnly = true,
                secure = true
            )
        )
        
        call.respond(HttpStatusCode.OK, mapOf("message" to "Déconnecté"))
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

