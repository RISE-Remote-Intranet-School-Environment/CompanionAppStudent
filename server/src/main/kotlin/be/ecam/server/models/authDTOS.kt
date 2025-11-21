package be.ecam.server.models

import kotlinx.serialization.Serializable

// its better to keep DTOs in separate files for clarity
// === DATA TRANSFER OBJECT (DTO) ===

// here we define the data structures used for communication in auth routes
@Serializable 
data class RegisterRequest(
    val username: String, 
    val email: String, 
    val password: String)

// DTO for login request
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
    val message: String)



