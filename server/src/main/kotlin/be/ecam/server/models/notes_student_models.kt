package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table pour les notes des étudiants
object NotesStudentsTable : IntIdTable("notes_students") {
    val studentId = integer("student_id")
    val academicYear = varchar("academic_year", 20)
    val formationId = varchar("formation_id", 50)
    val blocId = varchar("bloc_id", 50)
    val courseId = varchar("course_id", 50)
    val courseTitle = varchar("course_title", 255)
    val courseEcts = double("course_ects")
    val coursePeriod = varchar("course_period", 50)
    val courseId1 = varchar("course_id_1", 50).nullable()
    val courseSessionJan = double("course_session_jan").nullable()
    val courseSessionJun = double("course_session_jun").nullable()
    val courseSessionSep = double("course_session_sep").nullable()
    val componentCode = varchar("component_code", 50).nullable()
    val componentTitle = varchar("component_title", 255).nullable()
    val componentWeight = double("component_weight").nullable()
    val componentSessionJan = double("component_session_jan").nullable()
    val componentSessionJun = double("component_session_jun").nullable()
    val componentSessionSep = double("component_session_sep").nullable()
}

// DTO pour exposer les notes des étudiants au front
@Serializable
data class NotesStudentDTO(
    val id: Int,
    val studentId: Int,
    val academicYear: String,
    val formationId: String,
    val blocId: String,
    val courseId: String,
    val courseTitle: String,
    val courseEcts: Double,
    val coursePeriod: String,
    val courseId1: String?,
    val courseSessionJan: Double?,
    val courseSessionJun: Double?,
    val courseSessionSep: Double?,
    val componentCode: String?,
    val componentTitle: String?,
    val componentWeight: Double?,
    val componentSessionJan: Double?,
    val componentSessionJun: Double?,
    val componentSessionSep: Double?
)

// mapper ResultRow -> DTO
fun ResultRow.toNotesStudentDTO() = NotesStudentDTO(
    id = this[NotesStudentsTable.id].value,
    studentId = this[NotesStudentsTable.studentId],
    academicYear = this[NotesStudentsTable.academicYear],
    formationId = this[NotesStudentsTable.formationId],
    blocId = this[NotesStudentsTable.blocId],
    courseId = this[NotesStudentsTable.courseId],
    courseTitle = this[NotesStudentsTable.courseTitle],
    courseEcts = this[NotesStudentsTable.courseEcts],
    coursePeriod = this[NotesStudentsTable.coursePeriod],
    courseId1 = this[NotesStudentsTable.courseId1],
    courseSessionJan = this[NotesStudentsTable.courseSessionJan],
    courseSessionJun = this[NotesStudentsTable.courseSessionJun],
    courseSessionSep = this[NotesStudentsTable.courseSessionSep],
    componentCode = this[NotesStudentsTable.componentCode],
    componentTitle = this[NotesStudentsTable.componentTitle],
    componentWeight = this[NotesStudentsTable.componentWeight],
    componentSessionJan = this[NotesStudentsTable.componentSessionJan],
    componentSessionJun = this[NotesStudentsTable.componentSessionJun],
    componentSessionSep = this[NotesStudentsTable.componentSessionSep]
)

// DTO pour écriture (création et update complet)
@Serializable
data class NotesStudentWriteRequest(
    val studentId: Int,
    val academicYear: String,
    val formationId: String,
    val blocId: String,
    val courseId: String,
    val courseTitle: String,
    val courseEcts: Double,
    val coursePeriod: String,
    val courseId1: String? = null,
    val courseSessionJan: Double? = null,
    val courseSessionJun: Double? = null,
    val courseSessionSep: Double? = null,
    val componentCode: String? = null,
    val componentTitle: String? = null,
    val componentWeight: Double? = null,
    val componentSessionJan: Double? = null,
    val componentSessionJun: Double? = null,
    val componentSessionSep: Double? = null
)
