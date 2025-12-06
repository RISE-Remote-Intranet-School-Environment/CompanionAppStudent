package be.ecam.server.models

import kotlinx.serialization.Serializable

// ========= Requêtes d'auth =========

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

// ========= Réponses d'auth =========

// Infos de l'utilisateur authentifié (ce qu'on renvoie au front)
@Serializable
data class AuthUserDTO(
    val id: Int,
    val username: String,
    val email: String,
    val role: UserRole,       // <-- enum UserRole @Serializable
    val avatarUrl: String? = null
)

// Réponse complète des routes /auth/*
@Serializable
data class AuthResponse(
    val user: AuthUserDTO,
    val message: String,
    val token: String
)
