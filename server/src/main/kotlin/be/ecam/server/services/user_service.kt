package be.ecam.server.services

import at.favre.lib.crypto.bcrypt.BCrypt
import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object UserService {

    fun getAllUsers(): List<UserPublicDTO> = transaction {
        UsersTable.selectAll().map { it.toUser().toPublicDTO() }
    }

    fun createUser(req: UserWriteRequest): UserPublicDTO = transaction {
        val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())

        val newId = UsersTable.insertAndGetId {
            it[username] = req.username
            it[email] = req.email
            it[passwordHash] = hash
            it[firstName] = req.firstName
            it[lastName] = req.lastName
            it[role] = req.role
            it[avatarUrl] = req.avatarUrl
            it[professorId] = req.professorId
            it[studentId] = req.studentId
        }

        UsersTable
            .selectAll()
            .where { UsersTable.id eq newId }
            .single()
            .toUser()
            .toPublicDTO()
    }

    fun getUserById(id: Int): UserPublicDTO? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.toUser()
            ?.toPublicDTO()
    }

    fun getUserByEmail(email: String): UserPublicDTO? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()
            ?.toUser()
            ?.toPublicDTO()
    }

    fun getUserByUsername(username: String): UserPublicDTO? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.username eq username }
            .singleOrNull()
            ?.toUser()
            ?.toPublicDTO()
    }

    fun updateUser(id: Int, req: UpdateUserRequest): UserPublicDTO? = transaction {
        val updated = UsersTable.update({ UsersTable.id eq id }) {
            req.username?.let { v -> it[username] = v }
            req.email?.let { v -> it[email] = v }
            req.password?.let { v ->
                it[passwordHash] = BCrypt.withDefaults().hashToString(12, v.toCharArray())
            }
            //  CORRECTION : Utiliser une approche différente pour les champs nullable
            if (req.firstName != null) {
                it[firstName] = req.firstName
            }
            if (req.lastName != null) {
                it[lastName] = req.lastName
            }
            req.role?.let { v -> it[role] = v }
            if (req.avatarUrl != null) {
                it[avatarUrl] = req.avatarUrl
            }
            if (req.professorId != null) {
                it[professorId] = req.professorId
            }
            if (req.studentId != null) {
                it[studentId] = req.studentId
            }
        }

        if (updated == 0) return@transaction null

        UsersTable
            .selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.toUser()
            ?.toPublicDTO()
    }

    fun deleteUser(id: Int): Boolean = transaction {
        UsersTable.deleteWhere { UsersTable.id eq id } > 0
    }

    fun attachStudent(userId: Int, studentId: Int): UserPublicDTO? = transaction {
        val updated = UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.studentId] = studentId
        }

        if (updated == 0) return@transaction null

        UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.toUser()
            ?.toPublicDTO()
    }

    fun attachProfessor(userId: Int, professorId: Int): UserPublicDTO? = transaction {
        val updated = UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.professorId] = professorId
        }

        if (updated == 0) return@transaction null

        UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.toUser()
            ?.toPublicDTO()
    }

    fun updateAvatar(userId: Int, avatarUrl: String): UserPublicDTO? = transaction {
        val updated = UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.avatarUrl] = avatarUrl
        }

        if (updated == 0) return@transaction null

        UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.toUser()
            ?.toPublicDTO()
    }
}