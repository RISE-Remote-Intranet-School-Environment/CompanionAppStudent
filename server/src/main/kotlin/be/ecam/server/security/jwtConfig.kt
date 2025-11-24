package be.ecam.server.security

object JwtConfig {
    const val issuer = "ecam.server"
    const val audience = "ecam.client"
    const val realm = "Access to admin API"

    // TODO: change this secret for production use!
    const val secret = "SUPER_SECRET_KEY_CHANGE_ME"
}
