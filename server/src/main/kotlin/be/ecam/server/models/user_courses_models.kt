package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table pour stocker les cours sélectionnés par chaque utilisateur
object UserCoursesTable : IntIdTable("user_courses") {
    val userId = integer("user_id").references(UsersTable.id)
    val courseId = varchar("course_id", 50) // Ex: "4eial40"
    val addedAt = long("added_at").default(System.currentTimeMillis())
}

@Serializable
data class UserCourseDTO(
    val id: Int,
    val userId: Int,
    val courseId: String,
    val addedAt: Long
)

fun ResultRow.toUserCourseDTO() = UserCourseDTO(
    id = this[UserCoursesTable.id].value,
    userId = this[UserCoursesTable.userId],
    courseId = this[UserCoursesTable.courseId],
    addedAt = this[UserCoursesTable.addedAt]
)

@Serializable
data class AddCourseRequest(
    val courseId: String
)

@Serializable
data class AddCoursesRequest(
    val courseIds: List<String>
)