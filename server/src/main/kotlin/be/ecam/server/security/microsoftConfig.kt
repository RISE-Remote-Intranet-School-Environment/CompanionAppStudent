package be.ecam.server.security

import java.io.File

object MicrosoftConfig {
    
    val clientId: String by lazy {
        // 1. Variable d'env directe (dev local)
        val envValue = System.getenv("MS_CLIENT_ID")
        if (!envValue.isNullOrBlank()) return@lazy envValue

        // 2. Fichier pointé par MS_CLIENT_ID_FILE (prod avec sops)
        val filePath = System.getenv("MS_CLIENT_ID_FILE")
        if (!filePath.isNullOrBlank()) {
            try {
                return@lazy File(filePath).readText().trim()
            } catch (e: Exception) {
                System.err.println("Cannot read MS_CLIENT_ID_FILE: $filePath")
            }
        }

        error("MS_CLIENT_ID not configured")
    }

    val clientSecret: String by lazy {
        // 1. Variable d'env directe (dev local)
        val envValue = System.getenv("MS_CLIENT_SECRET")
        if (!envValue.isNullOrBlank()) return@lazy envValue

        // 2. Fichier pointé par MS_CLIENT_SECRET_FILE (prod avec sops)
        val filePath = System.getenv("MS_CLIENT_SECRET_FILE")
        if (!filePath.isNullOrBlank()) {
            try {
                return@lazy File(filePath).readText().trim()
            } catch (e: Exception) {
                System.err.println("Cannot read MS_CLIENT_SECRET_FILE: $filePath")
            }
        }

        error("MS_CLIENT_SECRET not configured")
    }

    const val redirectUri = "https://clacoxygen.msrl.be/api/auth/microsoft/callback"
    // User.Read est nécessaire pour la photo (/me/photo/$value). offline_access pour le refresh token.
    const val scope = "openid profile email User.Read offline_access"
}