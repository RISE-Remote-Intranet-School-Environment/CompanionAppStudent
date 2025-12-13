// services/auth_service.kt : service for authentication (register/login)

package be.ecam.server.services

import at.favre.lib.crypto.bcrypt.BCrypt
import be.ecam.server.models.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction

object AuthService {

    
    // register creates a new admin and returns a DTO 
    fun register(req: RegisterRequest): AuthUserDTO = transaction {

        // check uniquesness of username/email
        val exists = Admin.find {
            (AdminTable.username eq req.username) or (AdminTable.email eq req.email)
        }.firstOrNull()
        require(exists == null) { "Utilisateur déjà existant" }

        // hash mpd
        val hashed = BCrypt
            .withDefaults()
            .hashToString(12, req.password.toCharArray())

        val a = Admin.new {
            username = req.username
            email = req.email
            password = hashed
        }

        AuthUserDTO(
            id = a.id.value,
            username = a.username,
            email = a.email
        )
    }

    
    // login verifies credentials and returns a DTO
    fun login(req: LoginRequest): AuthUserDTO = transaction {
        val a = Admin.find {
            (AdminTable.username eq req.emailOrUsername) or
            (AdminTable.email eq req.emailOrUsername)
        }.firstOrNull() ?: error("Utilisateur introuvable")

        val ok = BCrypt
            .verifyer()
            .verify(req.password.toCharArray(), a.password)
            .verified

        require(ok) { "Mot de passe incorrect" }

        AuthUserDTO(
            id = a.id.value,
            username = a.username,
            email = a.email
        )
    }
}
