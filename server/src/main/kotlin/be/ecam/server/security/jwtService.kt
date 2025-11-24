package be.ecam.server.security

import be.ecam.server.models.AuthUserDTO
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtService {

    private val algorithm = Algorithm.HMAC256(JwtConfig.secret)

    fun generateToken(user: AuthUserDTO): String {
        val now = System.currentTimeMillis()

        return JWT.create()
            .withIssuer(JwtConfig.issuer)
            .withAudience(JwtConfig.audience)
            .withClaim("id", user.id)
            .withClaim("username", user.username)
            .withExpiresAt(Date(now + 1000 * 60 * 60 * 24)) // token valable 24h
            .sign(algorithm)
    }
}
