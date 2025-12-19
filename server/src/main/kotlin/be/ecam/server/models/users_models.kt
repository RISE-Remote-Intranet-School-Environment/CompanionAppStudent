package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow


// enum pour les rôles utilisateurs
@Serializable
enum class UserRole {
    ADMIN,
    PROF,
    STUDENT
}



// Table users
object UsersTable : IntIdTable("users") {
    val username = varchar("username", 100).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val firstName = varchar("first_name", 100).nullable()
    val lastName = varchar("last_name", 100).nullable()
    val role = enumerationByName<UserRole>("role", 20).default(UserRole.STUDENT)
    val avatarUrl = text("avatar_url").nullable()
    val professorId = integer("professor_id").nullable()
    val studentId = integer("student_id").nullable()
}


// model interne User
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val passwordHash: String,
    val firstName: String?,
    val lastName: String?,
    val role: UserRole,
    val avatarUrl: String?,
    val professorId: Int?,
    val studentId: Int?
)


// DTO public User sans le passwordHash
@Serializable
data class UserPublicDTO(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val role: UserRole,
    val avatarUrl: String?,
    val professorId: Int?,
    val studentId: Int?
)



// Mapping DB -> User interne
fun ResultRow.toUser(): User =
    User(
        id = this[UsersTable.id].value,
        username = this[UsersTable.username],
        email = this[UsersTable.email],
        passwordHash = this[UsersTable.passwordHash],
        firstName = this[UsersTable.firstName],
        lastName = this[UsersTable.lastName],
        role = this[UsersTable.role],
        avatarUrl = this[UsersTable.avatarUrl],
        professorId = this[UsersTable.professorId],
        studentId = this[UsersTable.studentId]
    )

// Mapping interne -> DTO public
fun User.toPublicDTO(): UserPublicDTO =
    UserPublicDTO(
        id = id,
        username = username,
        email = email,
        firstName = firstName,
        lastName = lastName,
        role = role,
        avatarUrl = avatarUrl,
        professorId = professorId,
        studentId = studentId
    )


// DTO pour création complète (ADMIN)
@Serializable
data class UserWriteRequest(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val role: UserRole,
    val avatarUrl: String? = null,
    val professorId: Int? = null,
    val studentId: Int? = null
)

// DTO pour update partiel (PATCH /users/{id})
@Serializable
data class UpdateUserRequest(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val role: UserRole? = null,
    val avatarUrl: String? = null,
    val professorId: Int? = null,
    val studentId: Int? = null
)

// DTO pour attacher un student
@Serializable
data class AttachStudentRequest(
    val studentId: Int
)

// DTO pour attacher un professeur
@Serializable
data class AttachProfessorRequest(
    val professorId: Int
)

// DTO pour que l'utilisateur mette à jour son avatar (route /users/me/avatar)
@Serializable
data class UpdateAvatarRequest(
    val avatarUrl: String
)


// wrappers de réponses avec DTO public
@Serializable
data class UserPublicResponse(
    val user: UserPublicDTO
)

@Serializable
data class UsersListResponse(
    val users: List<UserPublicDTO>
)

@Serializable
data class UserPublicMessageResponse(
    val user: UserPublicDTO,
    val message: String
)

@Serializable
data class UsersListMessageResponse(
    val users: List<UserPublicDTO>,
    val message: String
)
