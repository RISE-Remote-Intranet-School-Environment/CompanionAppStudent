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
    val message: String
)

class LoginViewModel : ViewModel() {
private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    // États observables
    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf("")
        private set

    var loginSuccess by mutableStateOf(false)
        private set


    fun register(username: String, email: String, password: String, baseUrl: String = "http://localhost:8082") {
    viewModelScope.launch {
        isLoading = true
        errorMessage = ""
        loginSuccess = false

        try {
            val response = client.post("$baseUrl/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(username, email, password))
            }

            if (response.status == HttpStatusCode.OK) {
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
        baseUrl: String = "http://localhost:8082",
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

                if (response.status == HttpStatusCode.OK) {
                    loginSuccess = true
                } else {
                    val message = response.bodyAsText()
                    errorMessage = "Erreur (${response.status.value}): $message"
                }

            } catch (e: Exception) {
                errorMessage = "Erreur de connexion : ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
