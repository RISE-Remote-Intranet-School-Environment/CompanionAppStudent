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

        // 1. Redirige vers Microsoft avec state pour savoir d'où vient la requête
        get("/login") {
            // Détecter si la requête vient du web (via Referer ou un paramètre)
            val platform = call.request.queryParameters["platform"] ?: "web"
            
            val authUrl = buildString {
                append("https://login.microsoftonline.com/common/oauth2/v2.0/authorize?")
                append("client_id=${MicrosoftConfig.clientId}")
                append("&response_type=code")
                append("&redirect_uri=${MicrosoftConfig.redirectUri}")
                append("&response_mode=query")
                append("&scope=${MicrosoftConfig.scope}")
                append("&state=$platform") // Passe la plateforme dans le state
            }
            call.respondRedirect(authUrl)
        }

        // 2. Callback de Microsoft
        get("/callback") {
            val code = call.parameters["code"]
            val error = call.parameters["error"]
            val errorDescription = call.parameters["error_description"]
            val stateParam = call.parameters["state"] ?: "web"

            // Parsing du state (format: "platform|returnUrl" ou juste "platform")
            val parts = stateParam.split("|")
            val platform = parts.getOrNull(0) ?: "web"
            val returnUrl = parts.getOrNull(1)

            // Fonction helper pour construire l'URL de redirection
            fun buildRedirectUrl(params: String): String {
                return when (platform) {
                    "android", "ios", "desktop" -> "be.ecam.companion://auth-callback?$params"
                    else -> {
                        // Web : on redirige vers la page HTML intermédiaire
                        // Si un returnUrl est fourni (ex: localhost:8080), on le passe en paramètre
                        val returnUrlParam = if (returnUrl != null) "&redirect_url=$returnUrl" else ""
                        "/auth-callback.html?$params$returnUrlParam"
                    }
                }
            }

            if (error != null) {
                val redirectUrl = buildRedirectUrl("error=$error&message=${errorDescription?.take(100) ?: ""}")
                call.respondRedirect(redirectUrl)
                return@get
            }

            if (code == null) {
                val redirectUrl = buildRedirectUrl("error=missing_code")
                call.respondRedirect(redirectUrl)
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
                    val redirectUrl = buildRedirectUrl("error=invalid_domain&message=Seuls les comptes ECAM sont autorisés")
                    call.respondRedirect(redirectUrl)
                    return@get
                }

                // Login ou création automatique de l'utilisateur
                val authResponse = AuthService.loginOrRegisterMicrosoft(
                    email = email,
                    firstName = profile.givenName ?: "",
                    lastName = profile.surname ?: "",
                    displayName = profile.displayName
                )

                // Redirection avec les tokens
                val redirectUrl = buildRedirectUrl(
                    "accessToken=${authResponse.accessToken}&refreshToken=${authResponse.refreshToken}"
                )
                call.respondRedirect(redirectUrl)

            } catch (e: Exception) {
                e.printStackTrace()
                val redirectUrl = buildRedirectUrl("error=server_error&message=${e.message?.take(100) ?: "Erreur inconnue"}")
                call.respondRedirect(redirectUrl)
            }
        }
    }
}