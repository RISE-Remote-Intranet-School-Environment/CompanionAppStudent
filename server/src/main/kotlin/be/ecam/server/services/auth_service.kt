package be.ecam.server.services

import at.favre.lib.crypto.bcrypt.BCrypt
import be.ecam.server.models.Admin
import be.ecam.server.models.AdminTable
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction

class AuthService {
    fun register(username: String, email: String, plain: String): Admin {
        val hash = BCrypt.withDefaults().hashToString(12, plain.toCharArray())
        return transaction {
            val exists = Admin.find { (AdminTable.username eq username) or (AdminTable.email eq email) }.empty().not()
            require(!exists) { "Username ou email déjà utilisé" }
            Admin.new {
                this.username = username
                this.email = email
                this.password = hash
            }
        }
    }

    fun login(emailOrUsername: String, plain: String): Admin? = transaction {
        val u = Admin.find {
            (AdminTable.email eq emailOrUsername) or (AdminTable.username eq emailOrUsername)
        }.firstOrNull() ?: return@transaction null
        if (BCrypt.verifyer().verify(plain.toCharArray(), u.password).verified) u else null
    }
}

