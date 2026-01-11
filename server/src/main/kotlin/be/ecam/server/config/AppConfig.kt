package be.ecam.server.config

/**
 * Configuration centralisée de l'application Clacoxygen.
 * Les valeurs sont lues depuis les variables d'environnement (définies par NixOS).
 */
object AppConfig {
    
    /** Domaine de l'application (ex: clacoxygen.msrl.be) */
    val domain: String by lazy {
        System.getenv("APP_DOMAIN") ?: "localhost"
    }
    
    /** URL de base complète (ex: https://clacoxygen.msrl.be) */
    val baseUrl: String by lazy {
        System.getenv("APP_BASE_URL") 
            ?: if (domain == "localhost") "http://localhost:28088" else "https://$domain"
    }
    
    /** Port du serveur */
    val port: Int by lazy {
        System.getenv("PORT")?.toIntOrNull() ?: 28088
    }
    
    /** Mode développement */
    val isDevelopment: Boolean by lazy {
        domain == "localhost" || System.getenv("DEV_MODE") == "true"
    }
    
    /** Nom de l'application */
    const val APP_NAME = "ClacOxygen"
    const val APP_DESCRIPTION = "ECAM Student Companion App"
    const val APP_VERSION = "1.0.0"
}