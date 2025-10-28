package be.ecam.server.models
import kotlinx.serialization.Serializable

@Serializable data class RegisterRequest(val username: String, val email: String, val password: String)
@Serializable data class LoginRequest(val emailOrUsername: String, val password: String)
@Serializable data class AuthUserDTO(val id: Int, val username: String, val email: String)
@Serializable data class AuthResponse(val user: AuthUserDTO, val message: String)


