package be.ecam.server.security

import be.ecam.server.models.AuthUserDTO
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtService {

    private val algorithm = Algorithm.HMAC256(JwtConfig.secret)

    private const val EXPIRATION_MS = 1000L * 60 * 60 * 24 // 24h

    fun generateToken(user: AuthUserDTO): String {
        val now = Date()

        return JWT.create()
            .withIssuer(JwtConfig.issuer)
            .withAudience(JwtConfig.audience)

            // Sujet du token 
            .withSubject(user.id.toString())

            // Claims usuels
            .withClaim("id", user.id)
            .withClaim("username", user.username)
            .withClaim("email", user.email)

            // Claim CRUCIAL pour gérer les droits admin / prof / student
            .withClaim("role", user.role.name)

            // timestamps
            .withIssuedAt(now)
            .withExpiresAt(Date(now.time + EXPIRATION_MS))

            .sign(algorithm)
    }
}
