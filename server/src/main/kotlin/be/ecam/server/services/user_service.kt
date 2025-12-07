package be.ecam.server.services

import at.favre.lib.crypto.bcrypt.BCrypt
import be.ecam.server.models.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object UserService {


    fun createUser(req: UserWriteRequest): UserPublicDTO = transaction {
        val hashed = BCrypt
            .withDefaults()
            .hashToString(12, req.password.toCharArray())

        val newId = UsersTable.insertAndGetId { row ->
            row[UsersTable.username] = req.username.trim()
            row[UsersTable.email] = req.email.trim()
            row[UsersTable.passwordHash] = hashed
            row[UsersTable.firstName] = req.firstName.trim()
            row[UsersTable.lastName] = req.lastName.trim()
            row[UsersTable.role] = req.role
            row[UsersTable.avatarUrl] = req.avatarUrl
            row[UsersTable.professorId] = req.professorId?.let { EntityID(it, ProfessorsTable) }
            row[UsersTable.studentId] = req.studentId?.let { EntityID(it, StudentsTable) }
        }

        UsersTable
            .selectAll()
            .where { UsersTable.id eq newId }
            .single()
            .toUser()
            .toPublicDTO()
    }


    fun getAllUsers(): List<UserPublicDTO> = transaction {
        UsersTable
            .selectAll()
            .orderBy(UsersTable.id to SortOrder.ASC)
            .map { row -> row.toUser().toPublicDTO() }
    }

    fun getUserById(id: Int): UserPublicDTO? = transaction {
        UsersTable
            .selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.toUser()
            ?.toPublicDTO()
    }

    // partial update user
    fun updateUser(id: Int, req: UpdateUserRequest): UserPublicDTO? = transaction {
        // Vérifier que le user existe
        val existingRow = UsersTable
            .selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?: return@transaction null

        // Si aucune info à modifier → renvoyer l’existant
        val hasAnyChange =
            req.username != null ||
            req.email != null ||
            req.firstName != null ||
            req.lastName != null ||
            req.role != null ||
            req.password != null ||
            req.avatarUrl != null ||
            req.professorId != null ||
            req.studentId != null

        if (!hasAnyChange) {
            return@transaction existingRow.toUser().toPublicDTO()
        }

        // Effectuer l’UPDATE via Exposed
        UsersTable.update({ UsersTable.id eq id }) { row ->
            req.username?.let { row[UsersTable.username] = it.trim() }
            req.email?.let { row[UsersTable.email] = it.trim() }
            req.firstName?.let { row[UsersTable.firstName] = it.trim() }
            req.lastName?.let { row[UsersTable.lastName] = it.trim() }
            req.role?.let { row[UsersTable.role] = it }        // enum UserRole

            req.avatarUrl?.let { row[UsersTable.avatarUrl] = it }

            req.professorId?.let {
                row[UsersTable.professorId] = EntityID(it, ProfessorsTable)
            }
            req.studentId?.let {
                row[UsersTable.studentId] = EntityID(it, StudentsTable)
            }

            req.password?.let { clear ->
                val newHashed = BCrypt
                    .withDefaults()
                    .hashToString(12, clear.toCharArray())
                row[UsersTable.passwordHash] = newHashed
            }
        }

        // Relire après update
        UsersTable
            .selectAll()
            .where { UsersTable.id eq id }
            .singleOrNull()
            ?.toUser()
            ?.toPublicDTO()
    }

    // delete user
    fun deleteUser(id: Int): Boolean = transaction {
        UsersTable.deleteWhere { UsersTable.id eq id } > 0
    }

    // attach student to user
    fun attachStudent(userId: Int, studentId: Int): UserPublicDTO? = transaction {
        // Vérifier que le student existe
        val studentExists = StudentsTable
            .selectAll()
            .where { StudentsTable.id eq studentId }
            .any()

        if (!studentExists) return@transaction null

        // 2) Vérifier que le user existe
        val userExists = UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .any()

        if (!userExists) return@transaction null

        // Mettre à jour l’utilisateur
        UsersTable.update({ UsersTable.id eq userId }) { row ->
            row[UsersTable.studentId] = EntityID(studentId, StudentsTable)
            row[UsersTable.role] = UserRole.STUDENT
        }

        // Relire le user
        UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.toUser()
            ?.toPublicDTO()
    }

    // attach professor to user
    fun attachProfessor(userId: Int, professorId: Int): UserPublicDTO? = transaction {
        // Vérifier que le prof existe
        val profExists = ProfessorsTable
            .selectAll()
            .where { ProfessorsTable.id eq professorId }
            .any()

        if (!profExists) return@transaction null

        // Vérifier que le user existe
        val userExists = UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .any()

        if (!userExists) return@transaction null

        // Mettre à jour l’utilisateur
        UsersTable.update({ UsersTable.id eq userId }) { row ->
            row[UsersTable.professorId] = EntityID(professorId, ProfessorsTable)
            row[UsersTable.role] = UserRole.PROF
            
        }

        // Relire le user
        UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.toUser()
            ?.toPublicDTO()
    }

    // modify avatar URL
    fun updateAvatar(userId: Int, avatarUrl: String): UserPublicDTO? = transaction {
        val exists = UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?: return@transaction null

        UsersTable.update({ UsersTable.id eq userId }) { row ->
            row[UsersTable.avatarUrl] = avatarUrl
        }

        UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.toUser()
            ?.toPublicDTO()
    }
}
