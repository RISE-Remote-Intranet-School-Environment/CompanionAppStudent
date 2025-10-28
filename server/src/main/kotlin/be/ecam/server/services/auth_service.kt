package be.ecam.server.services

import at.favre.lib.crypto.bcrypt.BCrypt // Service for authentication (register/login) of Admin users
import be.ecam.server.models.Admin // import the Admin entity
import be.ecam.server.models.AdminTable // import the AdminTable for queries
import org.jetbrains.exposed.sql.or // for combining query conditions
import org.jetbrains.exposed.sql.transactions.transaction // for db transactions

class AuthService {

    // Register a new admin user with hashed password
    fun register(username: String, email: String, plain: String): Admin {
        val hash = BCrypt.withDefaults().hashToString(12, plain.toCharArray())
        return transaction {
            val exists = Admin.find { (AdminTable.username eq username) or (AdminTable.email eq email) }.empty().not()
            require(!exists) { "Username or email already in use" }
            Admin.new {
                this.username = username
                this.email = email
                this.password = hash
            }
        }
    }

    // Login an admin user by verifying password
    fun login(emailOrUsername: String, plain: String): Admin? = transaction {
        val u = Admin.find {
            (AdminTable.email eq emailOrUsername) or (AdminTable.username eq emailOrUsername)
        }.firstOrNull() ?: return@transaction null
        if (BCrypt.verifyer().verify(plain.toCharArray(), u.password).verified) u else null
    }
}

