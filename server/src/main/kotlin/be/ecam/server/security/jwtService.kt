package be.ecam.server.security

import be.ecam.server.models.AuthUserDTO
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtService {
    private val algorithm = Algorithm.HMAC256(JwtConfig.secret)

    fun generateAccessToken(user: AuthUserDTO): String {
        val now = Date()
        return JWT.create()
            .withIssuer(JwtConfig.issuer)
            .withAudience(JwtConfig.audience)
            .withSubject(user.id.toString())
            .withClaim("id", user.id)
            .withClaim("role", user.role.name)
            .withIssuedAt(now)
            .withExpiresAt(Date(now.time + JwtConfig.ACCESS_TOKEN_EXPIRATION))
            .sign(algorithm)
    }

    fun generateRefreshToken(user: AuthUserDTO): String {
        // Le refresh token est opaque ou simple, ici un JWT long pour simplifier la validation
        val now = Date()
        return JWT.create()
            .withIssuer(JwtConfig.issuer)
            .withAudience(JwtConfig.audience)
            .withSubject(user.id.toString())
            .withIssuedAt(now)
            .withExpiresAt(Date(now.time + JwtConfig.REFRESH_TOKEN_EXPIRATION))
            .sign(algorithm)
    }
}