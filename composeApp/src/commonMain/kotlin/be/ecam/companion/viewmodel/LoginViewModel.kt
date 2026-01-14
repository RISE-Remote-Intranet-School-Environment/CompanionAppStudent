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

    init {
        // --- MODIFICATION ICI : On enregistre ce ViewModel pour qu'il reçoive les infos d'iOS ---
        AuthHelper.register(this)

        val savedToken = loadToken()
        if (!savedToken.isNullOrBlank()) {
            jwtToken = savedToken
            viewModelScope.launch {
                if (!validateAndRefreshToken(savedToken)) {
                    clearToken()
                    jwtToken = null
                }
            }
        }
    }

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

    /**
     * Valide le token et le rafraîchit si nécessaire
     * @return true si la session est valide, false sinon
     */
    private suspend fun validateAndRefreshToken(token: String): Boolean {
        return try {
            val response: HttpResponse = client.get("${defaultServerBaseUrl()}/api/auth/me") {
                val cleanToken = token.trim().removeSurrounding("\"")
                header(HttpHeaders.Authorization, "Bearer $cleanToken")
            }

            when (response.status.value) {
                in 200..299 -> {
                    currentUser = response.body()
                    loginSuccess = true
                    true
                }
                401 -> {
                    // Token expiré, tenter un refresh
                    refreshAccessToken()
                }
                else -> false
            }
        } catch (e: Exception) {
            println("Erreur de validation du token: ${e.message}")
            // Erreur réseau, on garde le token pour réessayer plus tard
            true
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

    suspend fun refreshAccessToken(): Boolean {
        val refreshToken = loadRefreshToken()

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

    suspend fun <T> executeWithRefresh(
        request: suspend () -> HttpResponse,
        onSuccess: suspend (HttpResponse) -> T,
        onError: (String) -> T
    ): T {
        val response = request()

        return when (response.status.value) {
            in 200..299 -> onSuccess(response)
            401 -> {
                if (refreshAccessToken()) {
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
                    jwtToken?.let {
                        header(HttpHeaders.Authorization, "Bearer ${it.trim().removeSurrounding("\"")}")
                    }
                }
            } catch (e: Exception) {
            }
        }

        jwtToken = null
        currentUser = null
        loginSuccess = false
        errorMessage = ""
        isLoading = false
        clearToken()
    }
}

// --- AJOUT : Le "Pont" pour Swift ---
object AuthHelper {
    private var viewModel: LoginViewModel? = null

    // Le ViewModel s'enregistre ici
    fun register(vm: LoginViewModel) {
        viewModel = vm
    }

    // iOS appelle cette fonction
    fun handleCallback(accessToken: String, refreshToken: String?) {
        // Sauvegarde immédiate
        be.ecam.companion.utils.saveToken(accessToken)
        if (refreshToken != null) {
            be.ecam.companion.utils.saveRefreshToken(refreshToken)
        }

        // Mise à jour de l'UI
        viewModel?.restoreSession(accessToken, refreshToken)
    }
}