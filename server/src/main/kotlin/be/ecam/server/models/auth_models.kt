package be.ecam.server.models

import kotlinx.serialization.Serializable



// /auth/register
@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

// /auth/login
@Serializable
data class LoginRequest(
    val emailOrUsername: String,
    val password: String
)



// ce qu'on renvoie au front concernant l'utilisateur authentifié
@Serializable
data class AuthUserDTO(
    val id: Int,
    val username: String,
    val email: String,
    val role: UserRole,       
    val avatarUrl: String? = null
)

// Réponse complète des routes /auth/*
@Serializable
data class AuthResponse(
    val user: AuthUserDTO,
    val message: String,
    val token: String
)
