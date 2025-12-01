package be.ecam.companion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
data class AuthUserDTO(
    val id: Int,
    val username: String,
    val email: String
)

@Serializable
data class AuthResponse(
    val user: AuthUserDTO? = null,
    val message: String,
    val token: String? = null
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


    fun register(username: String, email: String, password: String, baseUrl: String = "http://localhost:28088") {
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
     * 🔐 Login via POST /api/auth/login
     */
    fun login(
        baseUrl: String = "http://localhost:28088",
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

                    jwtToken = authResponse.token
                    currentUser = authResponse.user
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


    fun fetchMe(baseUrl: String = "http://localhost:28088") {
    viewModelScope.launch {
        if (jwtToken == null) {
            errorMessage = "Aucun token disponible"
            return@launch
        }

        try {
            val response: HttpResponse = client.get("$baseUrl/api/auth/me") {
                header("Authorization", "Bearer $jwtToken")
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
    baseUrl: String = "http://localhost:28088"
) {
    viewModelScope.launch {
        if (jwtToken == null) {
            errorMessage = "Aucun token disponible"
            return@launch
        }

        try {
            val response: HttpResponse = client.put("$baseUrl/api/auth/me") {
                header("Authorization", "Bearer $jwtToken")
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "username" to newUsername,
                        "email" to newEmail
                    )
                )
            }

            if (response.status.isSuccess()) {
                currentUser = response.body()
            } else {
                errorMessage = "Erreur updateMe : ${response.bodyAsText()}"
            }
        } catch (e: Exception) {
            errorMessage = "Erreur updateMe : ${e.message}"
        }
    }
}


}
