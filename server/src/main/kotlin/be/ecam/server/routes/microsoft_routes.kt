package be.ecam.server.routes

import be.ecam.server.security.MicrosoftConfig
import be.ecam.server.services.AuthService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MicrosoftTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null
)

@Serializable
data class MicrosoftProfile(
    val id: String,
    val mail: String? = null,
    val userPrincipalName: String? = null,
    val givenName: String? = null,
    val surname: String? = null,
    val displayName: String? = null
)

fun Route.microsoftAuthRoutes() {
    
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    route("/auth/microsoft") {

        // 1. Redirige vers Microsoft
        get("/login") {
            val authUrl = buildString {
                append("https://login.microsoftonline.com/common/oauth2/v2.0/authorize?")
                append("client_id=${MicrosoftConfig.clientId}")
                append("&response_type=code")
                append("&redirect_uri=${MicrosoftConfig.redirectUri}")
                append("&response_mode=query")
                append("&scope=${MicrosoftConfig.scope}")
            }
            call.respondRedirect(authUrl)
        }

        // 2. Callback de Microsoft
        get("/callback") {
            val code = call.parameters["code"]
            val error = call.parameters["error"]
            val errorDescription = call.parameters["error_description"]

            if (error != null) {
                call.respondRedirect("be.ecam.companion://auth-callback?error=$error&message=$errorDescription")
                return@get
            }

            if (code == null) {
                call.respondRedirect("be.ecam.companion://auth-callback?error=missing_code")
                return@get
            }

            try {
                // Échange code -> token
                val tokenResponse: MicrosoftTokenResponse = httpClient.submitForm(
                    url = "https://login.microsoftonline.com/common/oauth2/v2.0/token",
                    formParameters = Parameters.build {
                        append("client_id", MicrosoftConfig.clientId)
                        append("scope", MicrosoftConfig.scope)
                        append("code", code)
                        append("redirect_uri", MicrosoftConfig.redirectUri)
                        append("grant_type", "authorization_code")
                        append("client_secret", MicrosoftConfig.clientSecret)
                    }
                ).body()

                // Récupération du profil via Graph API
                val profile: MicrosoftProfile = httpClient.get("https://graph.microsoft.com/v1.0/me") {
                    header(HttpHeaders.Authorization, "Bearer ${tokenResponse.accessToken}")
                }.body()

                val email = profile.mail ?: profile.userPrincipalName ?: ""
                
                // Optionnel : Restriction aux emails ECAM
                if (!email.endsWith("@ecam.be")) {
                    call.respondRedirect("be.ecam.companion://auth-callback?error=invalid_domain")
                    return@get
                }

                // Login ou création automatique de l'utilisateur
                val authResponse = AuthService.loginOrRegisterMicrosoft(
                    email = email,
                    firstName = profile.givenName ?: "",
                    lastName = profile.surname ?: "",
                    displayName = profile.displayName
                )

                // Redirection vers l'app via Deep Link
                call.respondRedirect(
                    "be.ecam.companion://auth-callback?" +
                    "accessToken=${authResponse.accessToken}&" +
                    "refreshToken=${authResponse.refreshToken}"
                )

            } catch (e: Exception) {
                e.printStackTrace()
                call.respondRedirect("be.ecam.companion://auth-callback?error=server_error&message=${e.message}")
            }
        }
    }
}