package be.ecam.server.routes

import be.ecam.server.security.MicrosoftConfig
import be.ecam.server.services.AuthService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

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
            val platform = call.request.queryParameters["platform"] ?: "web"
            val returnUrl = call.request.queryParameters["returnUrl"] ?: ""
            val localCallback = call.request.queryParameters["localCallback"] ?: ""
            
            // Encode platform, returnUrl et localCallback dans le state (séparés par |)
            val state = buildString {
                append(platform)
                if (returnUrl.isNotBlank()) append("|$returnUrl")
                else if (localCallback.isNotBlank()) append("|$localCallback")
            }
            
            val authUrl = buildString {
                append("https://login.microsoftonline.com/common/oauth2/v2.0/authorize?")
                append("client_id=${MicrosoftConfig.clientId}")
                append("&response_type=code")
                append("&redirect_uri=${MicrosoftConfig.redirectUri}")
                append("&response_mode=query")
                append("&scope=${MicrosoftConfig.scope}")
                append("&state=$state")
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
            val parts = stateParam.split("|", limit = 2)
            val platform = parts.getOrNull(0) ?: "web"
            val returnUrl = parts.getOrNull(1)?.let { 
                try { java.net.URLDecoder.decode(it, "UTF-8") } catch (e: Exception) { it }
            }

            // Fonction helper pour construire l'URL de redirection
            fun buildRedirectUrl(params: String): String {
                return when (platform) {
                    // Android et iOS supportent les deep links
                    "android", "ios" -> "be.ecam.companion://auth-callback?$params"
                    
                    "desktop" -> {
                        // Si on a un callback local (serveur Desktop), l'utiliser
                        if (!returnUrl.isNullOrBlank() && returnUrl.startsWith("http://localhost")) {
                            val separator = if (returnUrl.contains("?")) "&" else "?"
                            "$returnUrl$separator$params"
                        } else {
                            // Sinon, page HTML de fallback
                            "/auth-callback-desktop.html?$params"
                        }
                    }
                    
                    else -> {
                        // Web : rediriger directement vers l'app avec les tokens en paramètres
                        if (!returnUrl.isNullOrBlank()) {
                            val separator = if (returnUrl.contains("?")) "&" else "?"
                            "$returnUrl$separator$params"
                        } else {
                            "/auth-callback.html?$params"
                        }
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
                println("Microsoft OAuth - Email: $email, Name: ${profile.givenName} ${profile.surname}")
                
                // Récupérer la photo de profil Microsoft (binaire)
                val avatarBase64: String? = try {
                    val photoResponse: HttpResponse = httpClient.get("https://graph.microsoft.com/v1.0/me/photo/\$value") {
                        header(HttpHeaders.Authorization, "Bearer ${tokenResponse.accessToken}")
                    }
                    println("Microsoft Photo Response Status: ${photoResponse.status}")
                    if (photoResponse.status.isSuccess()) {
                        val photoBytes: ByteArray = photoResponse.body()
                        println("Microsoft Photo Size: ${photoBytes.size} bytes")
                        val base64 = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(photoBytes)
                        println("Microsoft Photo Base64 length: ${base64.length}")
                        base64
                    } else {
                        println("Microsoft Photo not available (Status: ${photoResponse.status}). User might not have a profile picture set.")
                        null
                    }
                } catch (e: Exception) {
                    println("Impossible de récupérer la photo Microsoft: ${e.message}")
                    e.printStackTrace()
                    null
                }
                
                // Optionnel : Restriction aux emails ECAM
                if (!email.endsWith("@ecam.be")) {
                    val redirectUrl = buildRedirectUrl("error=invalid_domain&message=Seuls les comptes ECAM sont autorisés")
                    call.respondRedirect(redirectUrl)
                    return@get
                }

                // Login ou création automatique de l'utilisateur avec avatar
                val authResponse = AuthService.loginOrRegisterMicrosoft(
                    email = email,
                    firstName = profile.givenName ?: "",
                    lastName = profile.surname ?: "",
                    displayName = profile.displayName,
                    avatarUrl = avatarBase64 // Stocker en base64
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