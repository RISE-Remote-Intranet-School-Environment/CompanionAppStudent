package be.ecam.companion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.ecam.companion.data.defaultServerBaseUrl
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import be.ecam.companion.utils.saveToken
import be.ecam.companion.utils.loadToken
import be.ecam.companion.utils.clearToken
import be.ecam.companion.utils.saveRefreshToken
import be.ecam.companion.utils.loadRefreshToken 


@Serializable
data class LoginRequest(
    val emailOrUsername: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
enum class UserRole { ADMIN, PROF, STUDENT }


@Serializable
data class AuthUserDTO(
    val id: Int,
    val username: String,
    val email: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val firstName: String? = null,
    val lastName: String? = null

)

@Serializable
data class AuthResponse(
    val user: AuthUserDTO,
    val message: String,
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class UpdateMeResponse(
    val user: AuthUserDTO,
    val message: String
)

class LoginViewModel : ViewModel() {
    private val client = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) { json() }
    }

    // États observables
    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf("")
        private set

    var loginSuccess by mutableStateOf(false)
        private set

    var jwtToken by mutableStateOf<String?>(null)
        private set

    var currentUser by mutableStateOf<AuthUserDTO?>(null)
        private set


    fun register(username: String, email: String, password: String, baseUrl: String = defaultServerBaseUrl()) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = ""
            loginSuccess = false

            try {
                val response = client.post("$baseUrl/api/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody(RegisterRequest(username, email, password))
                }

                if (response.status.value in 200..299) {
                    loginSuccess = true
                } else {
                    val message = response.bodyAsText()
                    errorMessage = "Erreur (${response.status.value}): $message"
                }
            } catch (e: Exception) {
                errorMessage = "Erreur d'inscription : ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    init {
        val savedToken = loadToken()
        if (!savedToken.isNullOrBlank()) {
            jwtToken = savedToken
            viewModelScope.launch {
                val isValid = validateAndRefreshToken(savedToken)
                if (!isValid) {
                    logout()
                }
            }
        }
    }

    /**
     * Valide le token et le rafraîchit si nécessaire
     * @return true si la session est valide, false sinon
     */
    private suspend fun validateAndRefreshToken(token: String): Boolean {
        // 1. Vérification locale de l'expiration AVANT toute requête réseau
        if (isTokenExpiredLocally(token)) {
            println("Token expiré localement, tentative de refresh...")
            return refreshAccessToken()
        }
        
        return try {
            val response: HttpResponse = client.get("${defaultServerBaseUrl()}/api/auth/me") {
                val cleanToken = token.trim().removeSurrounding("\"")
                header(HttpHeaders.Authorization, "Bearer $cleanToken")
            }
            
            when (response.status.value) {
                in 200..299 -> {
                    currentUser = response.body()
                    // Sauvegarder dans le cache pour le mode offline
                    currentUser?.let { CacheHelper.save(CacheKeys.CURRENT_USER, it) }
                    loginSuccess = true
                    true
                }
                401 -> {
                    // Token rejeté par le serveur -> refresh obligatoire
                    val refreshed = refreshAccessToken()
                    if (!refreshed) {
                        println("Refresh token expiré ou invalide, déconnexion forcée")
                    }
                    refreshed
                }
                else -> {
                    println("Erreur serveur inattendue: ${response.status.value}")
                    false
                }
            }
        } catch (e: Exception) {
            println("Erreur réseau: ${e.message}")
            
            // En cas d'erreur réseau, on vérifie si le token est encore valide localement
            // avec une marge de sécurité (on considère expiré 1 minute avant la vraie expiration)
            val stillValidLocally = !isTokenExpiredLocally(token, marginSeconds = 60)
            
            if (stillValidLocally) {
                // Le token semble encore valide -> autoriser le mode offline
                // MAIS on ne charge pas currentUser depuis le cache pour éviter les incohérences
                // L'utilisateur verra une UI dégradée
                println("Mode offline activé - token localement valide")
                loginSuccess = true
                // Charger l'utilisateur depuis le cache offline si disponible
                loadCachedUser()
                true
            } else {
                // Token expiré ou bientôt expiré -> pas de mode offline
                println("Token expiré, mode offline refusé")
                false
            }
        }
    }

    /**
     * Vérifie si le JWT est expiré en décodant le payload localement
     * @param marginSeconds Marge de sécurité en secondes (défaut: 0)
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun isTokenExpiredLocally(token: String, marginSeconds: Long = 0): Boolean {
        return try {
            val cleanToken = token.trim().removeSurrounding("\"")
            val parts = cleanToken.split(".")
            if (parts.size != 3) return true
            
            // Décoder le payload (partie 2 du JWT)
            val payloadJson = Base64.UrlSafe.decode(parts[1]).decodeToString()
            val payload = Json.parseToJsonElement(payloadJson).jsonObject
            
            val exp = payload["exp"]?.jsonPrimitive?.content?.toLongOrNull()
                ?: return true
            
            val now = System.currentTimeMillis() / 1000
            val expiresAt = exp - marginSeconds
            
            now >= expiresAt
        } catch (e: Exception) {
            println("Erreur décodage JWT: ${e.message}")
            true // En cas de doute, considérer comme expiré
        }
    }

    /**
     * Charge l'utilisateur depuis le cache offline
     */
    private fun loadCachedUser() {
        currentUser = CacheHelper.load<AuthUserDTO>(CacheKeys.CURRENT_USER)
    }

    /**
     * Rafraîchit l'access token en utilisant le refresh token
     */
    suspend fun refreshAccessToken(): Boolean {
        val refreshToken = loadRefreshToken()
        
        // Si pas de refresh token stocké (ex: web avec cookie HttpOnly)
        // on tente quand même la requête car le cookie sera envoyé automatiquement
        
        return try {
            val response: HttpResponse = client.post("${defaultServerBaseUrl()}/api/auth/refresh") {
                contentType(ContentType.Application.Json)
                if (!refreshToken.isNullOrBlank()) {
                    setBody(mapOf("refreshToken" to refreshToken))
                } else {
                    setBody(emptyMap<String, String>())
                }
            }
            
            if (response.status.isSuccess()) {
                val authResponse: AuthResponse = response.body()
                jwtToken = authResponse.accessToken
                saveToken(authResponse.accessToken)
                
                if (authResponse.refreshToken.isNotBlank()) {
                    saveRefreshToken(authResponse.refreshToken)
                }
                
                currentUser = authResponse.user
                loginSuccess = true
                println("Token rafraîchi avec succès")
                true
            } else {
                println("Échec du refresh: ${response.status.value}")
                // Refresh échoué -> nettoyer les tokens
                clearToken()
                jwtToken = null
                currentUser = null
                loginSuccess = false
                false
            }
        } catch (e: Exception) {
            println("Erreur refresh token: ${e.message}")
            false
        }
    }

    /**
     * Restaure la session à partir d'un token stocké (utile après OAuth callback)
     */
    fun restoreSession(accessToken: String, refreshToken: String? = null) {
        jwtToken = accessToken
        saveToken(accessToken)
        
        // Sauvegarder le refresh token s'il est fourni
        if (!refreshToken.isNullOrBlank()) {
            saveRefreshToken(refreshToken)
        }
        
        viewModelScope.launch {
            try {
                val response: HttpResponse = client.get("${defaultServerBaseUrl()}/api/auth/me") {
                    val token = accessToken.trim().removeSurrounding("\"")
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                if (response.status.isSuccess()) {
                    currentUser = response.body()
                    loginSuccess = true
                } else {
                    logout()
                }
            } catch (e: Exception) {
                errorMessage = "Erreur de restauration : ${e.message}"
                logout()
            }
        }
    }

    /**
     * Login via POST /api/auth/login
     */
    fun login(
        baseUrl: String = defaultServerBaseUrl(),
        emailOrUsername: String,
        password: String
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = ""
            loginSuccess = false

            try {
                val response: HttpResponse = client.post("$baseUrl/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(emailOrUsername, password))
                }

                if (response.status.isSuccess()) {
                    val authResponse: AuthResponse = response.body()

                    jwtToken = authResponse.accessToken
                    currentUser = authResponse.user
                    
                    saveToken(authResponse.accessToken)
                    
                    // Le refresh token peut être vide si le serveur utilise des cookies
                    if (authResponse.refreshToken.isNotBlank()) {
                        saveRefreshToken(authResponse.refreshToken)
                    }
                    
                    loginSuccess = true
                } else {
                    errorMessage = "Erreur ${response.status.value}: ${response.bodyAsText()}"
                }
            } catch (e: Exception) {
                errorMessage = "Erreur de connexion : ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }


    fun fetchMe(baseUrl: String = defaultServerBaseUrl()) {
        viewModelScope.launch {
        if (jwtToken == null) {
            errorMessage = "Aucun token disponible"
            return@launch
        }

        try {
            val response: HttpResponse = client.get("$baseUrl/api/auth/me") {
                val token = jwtToken?.trim()?.removeSurrounding("\"")
                header(HttpHeaders.Authorization, "Bearer $token")
                //header("Authorization", "Bearer $jwtToken")
            }

            if (response.status.isSuccess()) {
                currentUser = response.body()
            } else {
                errorMessage = "Erreur : ${response.bodyAsText()}"
            }
        } catch (e: Exception) {
            errorMessage = "Erreur fetchMe : ${e.message}"
        }
    }
}

    fun updateMe(
        newUsername: String,
        newEmail: String,
        baseUrl: String = defaultServerBaseUrl()
    ) {
        viewModelScope.launch {
            val token = jwtToken?.trim()?.removeSurrounding("\"")
            if (token.isNullOrBlank()) {
                errorMessage = "Aucun token disponible"
                return@launch
            }

            try {
                val response: HttpResponse = client.put("$baseUrl/api/auth/me") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("username" to newUsername, "email" to newEmail))
                }

                if (response.status.isSuccess()) {
                    val updated: UpdateMeResponse = response.body()
                    currentUser = updated.user
                    
                } else {
                    errorMessage = "Erreur updateMe (${response.status.value}) : ${response.bodyAsText()}"
                }
            } catch (e: Exception) {
                errorMessage = "Erreur updateMe : ${e.message}"
            }
        }
    }

    /**
     * Rafraîchit l'access token en utilisant le refresh token
     * Pour le web, le refresh token est dans un cookie HttpOnly
     */
    suspend fun refreshAccessToken(): Boolean {
        val refreshToken = loadRefreshToken()
        
        return try {
            val response: HttpResponse = client.post("${defaultServerBaseUrl()}/api/auth/refresh") {
                contentType(ContentType.Application.Json)
                // Envoyer le refresh token dans le body si disponible (mobile/desktop)
                // Sinon le serveur utilisera le cookie (web)
                if (!refreshToken.isNullOrBlank()) {
                    setBody(mapOf("refreshToken" to refreshToken))
                } else {
                    setBody(emptyMap<String, String>())
                }
            }
            
            if (response.status.isSuccess()) {
                val authResponse: AuthResponse = response.body()
                jwtToken = authResponse.accessToken
                saveToken(authResponse.accessToken)
                
                // Mettre à jour le refresh token si fourni
                if (authResponse.refreshToken.isNotBlank()) {
                    saveRefreshToken(authResponse.refreshToken)
                }
                
                // Recharger les infos utilisateur
                fetchMe()
                loginSuccess = true
                true
            } else {
                clearToken()
                loginSuccess = false
                false
            }
        } catch (e: Exception) {
            println("Erreur refresh token: ${e.message}")
            false
        }
    }

    /**
     * Exécute une requête API avec refresh automatique en cas de 401
     */
    suspend fun <T> executeWithRefresh(
        request: suspend () -> HttpResponse,
        onSuccess: suspend (HttpResponse) -> T,
        onError: (String) -> T
    ): T {
        val response = request()
        
        return when (response.status.value) {
            in 200..299 -> onSuccess(response)
            401 -> {
                // Tenter un refresh
                if (refreshAccessToken()) {
                    // Réessayer la requête avec le nouveau token
                    val retryResponse = request()
                    if (retryResponse.status.isSuccess()) {
                        onSuccess(retryResponse)
                    } else {
                        onError("Erreur après refresh: ${retryResponse.status.value}")
                    }
                } else {
                    logout()
                    onError("Session expirée, veuillez vous reconnecter")
                }
            }
            else -> onError("Erreur ${response.status.value}: ${response.bodyAsText()}")
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                client.post("${defaultServerBaseUrl()}/api/auth/logout") {
                    contentType(ContentType.Application.Json)
                    jwtToken?.let {
                        header(HttpHeaders.Authorization, "Bearer ${it.trim().removeSurrounding("\"")}")
                    }
                    loadRefreshToken()?.let { refreshToken ->
                        setBody(mapOf("refreshToken" to refreshToken))
                    }
                }
            } catch (e: Exception) {
                // Ignorer les erreurs de logout côté serveur
                // (on nettoie localement de toute façon)
            }
        }
        
        jwtToken = null
        currentUser = null
        loginSuccess = false
        errorMessage = ""
        isLoading = false
        clearToken()
        
        OfflineCache.clear(CacheKeys.CURRENT_USER)
    }
}





