package be.ecam.server.security

object JwtConfig {
    const val issuer = "ecam.server"
    const val audience = "ecam.client"
    const val realm = "Access to admin API"

    // ⚠️ Mets ici une clé plus longue et secrète en prod
    const val secret = "SUPER_SECRET_KEY_CHANGE_ME"
}
