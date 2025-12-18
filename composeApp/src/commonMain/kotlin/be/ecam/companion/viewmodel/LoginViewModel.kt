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
    val avatarUrl: String? = null

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
        expectSuccess = false // let us handle non-2xx instead of throwing
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
                try {
                    val response: HttpResponse = client.get("${defaultServerBaseUrl()}/api/auth/me") {
                        val token = savedToken.trim().removeSurrounding("\"")
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }
                    if (response.status.isSuccess()) {
                        currentUser = response.body()
                        loginSuccess = true
                    } else {
                        // Token invalide, on le supprime
                        clearToken()
                        jwtToken = null
                    }
                } catch (e: Exception) {
                    // Erreur réseau, on garde le token pour réessayer plus tard
                    println("Erreur de validation du token: ${e.message}")
                }
            }
        }
    }

    /**
     * Restaure la session à partir d'un token stocké (utile après OAuth callback)
     */
    fun restoreSession(accessToken: String) {
        jwtToken = accessToken
        saveToken(accessToken)
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
     * 🔐 Login via POST /api/auth/login
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


    fun logout() {
        jwtToken = null
        currentUser = null
        loginSuccess = false
        errorMessage = ""
        isLoading = false
        clearToken()
    }

}





