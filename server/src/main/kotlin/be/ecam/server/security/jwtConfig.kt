package be.ecam.server.security

object JwtConfig {

    const val issuer = "be.ecam.server"
    const val audience = "be.ecam.client"
    const val realm = "ECAM Companion API"

   
    // - MINIMUM 32 chars sinon HMAC256 faiblesecurité
    const val secret = "CHANGE_THIS_TO_A_LONG_RANDOM_SECRET_KEY_32_CHARS_MIN"
}
