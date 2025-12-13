package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table pour les étudiants PAE
object PaeStudentsTable : IntIdTable("pae_students") {
    val studentId = integer("student_id")
    val studentName = varchar("student_name", 255)
    val email = varchar("email", 255)
    val role = varchar("role", 50).nullable()
    val program = varchar("program", 255).nullable()
    val enrolYear = integer("enrol_year").nullable()
    val formationId = varchar("formation_id", 50).nullable()
    val blocId = varchar("bloc_id", 50).nullable()
    val courseIds = text("course_ids").nullable()
}

// DTO pour exposer les étudiants PAE au front
@Serializable
data class PaeStudentDTO(
    val id: Int,
    val studentId: Int,
    val studentName: String,
    val email: String,
    val role: String?,
    val program: String?,
    val enrolYear: Int?,
    val formationId: String?,
    val blocId: String?,
    val courseIds: String?
)

// mapper ResultRow -> DTO
fun ResultRow.toPaeStudentDTO() = PaeStudentDTO(
    id = this[PaeStudentsTable.id].value,
    studentId = this[PaeStudentsTable.studentId],
    studentName = this[PaeStudentsTable.studentName],
    email = this[PaeStudentsTable.email],
    role = this[PaeStudentsTable.role],
    program = this[PaeStudentsTable.program],
    enrolYear = this[PaeStudentsTable.enrolYear],
    formationId = this[PaeStudentsTable.formationId],
    blocId = this[PaeStudentsTable.blocId],
    courseIds = this[PaeStudentsTable.courseIds]
)


// DTO pour écriture (création et update complet)
@Serializable
data class PaeStudentWriteRequest(
    val studentId: Int,
    val studentName: String,
    val email: String,
    val role: String? = null,
    val program: String? = null,
    val enrolYear: Int? = null,
    val formationId: String? = null,
    val blocId: String? = null,
    val courseIds: String? = null
)