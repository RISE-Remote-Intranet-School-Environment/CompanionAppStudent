package be.ecam.server.security

import java.io.File

object JwtConfig {
    const val issuer = "be.ecam.server"
    const val audience = "be.ecam.client"
    const val realm = "ECAM Companion API"

    // 15 minutes pour l'access token
    const val ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 15 
    // 7 jours pour le refresh token
    const val REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7

    // Charge le secret :
    // 1. Depuis la variable d'env JWT_SECRET (Dev / Docker simple)
    // 2. Depuis le fichier pointé par JWT_SECRET_FILE (Prod / Sops)
    // 3. Fallback (Dev uniquement)
    val secret: String by lazy {
        val envSecret = System.getenv("JWT_SECRET")
        if (!envSecret.isNullOrBlank()) return@lazy envSecret

        val secretFilePath = System.getenv("JWT_SECRET_FILE")
        if (!secretFilePath.isNullOrBlank()) {
            try {
                return@lazy File(secretFilePath).readText().trim()
            } catch (e: Exception) {
                System.err.println("Impossible de lire le fichier secret: $secretFilePath")
            }
        }

        "DEV_FALLBACK_SECRET_DO_NOT_USE_IN_PROD"
    }
}