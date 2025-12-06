package be.ecam.server.services

import at.favre.lib.crypto.bcrypt.BCrypt
import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction

object AuthService {

    // ==================
    // REGISTER
    // ==================
    // Crée un user dans UsersTable et renvoie un AuthUserDTO
    fun register(req: RegisterRequest): AuthUserDTO = transaction {

        val trimmedUsername = req.username.trim()
        val trimmedEmail = req.email.trim()

        // 1) vérifier unicité username / email
        val exists = UsersTable
            .selectAll()
            .where {
                (UsersTable.username eq trimmedUsername) or
                (UsersTable.email eq trimmedEmail)
            }
            .singleOrNull()

        require(exists == null) { "Utilisateur déjà existant" }

        // 2) hash du mot de passe
        val hashed = BCrypt
            .withDefaults()
            .hashToString(12, req.password.toCharArray())

        // Pour l’instant on force tous les nouveaux comptes en STUDENT
        val role = UserRole.STUDENT

        // 3) insert + récupération de l'id (IntIdTable -> EntityID<Int>.value)
        val newId = UsersTable.insertAndGetId { row ->
            row[UsersTable.username] = trimmedUsername
            row[UsersTable.email] = trimmedEmail
            row[UsersTable.passwordHash] = hashed
            row[UsersTable.firstName] = ""      // à remplir plus tard via /users PATCH
            row[UsersTable.lastName] = ""
            row[UsersTable.role] = role         // enum UserRole
            row[UsersTable.avatarUrl] = null
            row[UsersTable.professorId] = null
            row[UsersTable.studentId] = null
        }.value

        AuthUserDTO(
            id = newId,
            username = trimmedUsername,
            email = trimmedEmail,
            role = role,
            avatarUrl = null
        )
    }

    // ==================
    // LOGIN
    // ==================
    // email ou username + password → AuthUserDTO
    fun login(req: LoginRequest): AuthUserDTO = transaction {

        val identifier = req.emailOrUsername.trim()

        val row = UsersTable
            .selectAll()
            .where {
                (UsersTable.username eq identifier) or
                (UsersTable.email eq identifier)
            }
            .singleOrNull()
            ?: error("Utilisateur introuvable")

        val ok = BCrypt
            .verifyer()
            .verify(req.password.toCharArray(), row[UsersTable.passwordHash])
            .verified

        require(ok) { "Mot de passe incorrect" }

        val roleEnum: UserRole = row[UsersTable.role]  // enum direct grâce à enumerationByName

        AuthUserDTO(
            id = row[UsersTable.id].value,          // IntIdTable -> .value
            username = row[UsersTable.username],
            email = row[UsersTable.email],
            role = roleEnum,
            avatarUrl = row[UsersTable.avatarUrl]
        )
    }
}
