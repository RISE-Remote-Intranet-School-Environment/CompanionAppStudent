package be.ecam.server.models

import kotlinx.serialization.Serializable


// /api/auth.register
@Serializable 
data class RegisterRequest(
    val username: String, 
    val email: String, 
    val password: String)


// /api/auth.login
@Serializable 
data class LoginRequest(
    val emailOrUsername: String,
    val password: String)

// DTO for authenticated user information
@Serializable 
data class AuthUserDTO(
    val id: Int, 
    val username: String, 
    val email: String)

// DTO for authentication response 
@Serializable 
data class AuthResponse(
    val user: AuthUserDTO, 
    val message: String,
    val token: String)



