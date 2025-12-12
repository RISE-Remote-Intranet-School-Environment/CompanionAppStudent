package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

/**
 * Table des soumissions / dépôts étudiants
 * (rapports, projets, fichiers PDF, images, ZIP...)
 */
object StudentSubmissionsTable : IntIdTable("student_submissions") {

    // FK réelle vers student (OK)
    val student = reference("student_id", StudentsTable)

    // Codes logiques (comme dans tes données)
    val courseId = varchar("course_id", 50).nullable()       // ex: "course_123"
    val sousCourseId = varchar("sous_course_id", 50).nullable() // ex: "sous_course_45"

    // Infos fichier
    val title = varchar("title", 255)
    val fileUrl = varchar("file_url", 1024)
    val mimeType = varchar("mime_type", 100)
    val uploadedAt = long("uploaded_at")

    // Annotation / correction
    val grade = varchar("grade", 20).nullable()
    val feedback = text("feedback").nullable()
}

/**
 * DTO public pour le frontend
 */
@Serializable
data class StudentSubmissionDTO(
    val id: Int,
    val studentId: Int,
    val courseId: String?,
    val sousCourseId: String?,
    val title: String,
    val fileUrl: String,
    val mimeType: String,
    val uploadedAt: Long,
    val grade: String?,
    val feedback: String?
)

/**
 * Mapper DB -> DTO
 */
fun ResultRow.toStudentSubmissionDTO() = StudentSubmissionDTO(
    id = this[StudentSubmissionsTable.id].value,
    studentId = this[StudentSubmissionsTable.student].value,
    courseId = this[StudentSubmissionsTable.courseId],
    sousCourseId = this[StudentSubmissionsTable.sousCourseId],
    title = this[StudentSubmissionsTable.title],
    fileUrl = this[StudentSubmissionsTable.fileUrl],
    mimeType = this[StudentSubmissionsTable.mimeType],
    uploadedAt = this[StudentSubmissionsTable.uploadedAt],
    grade = this[StudentSubmissionsTable.grade],
    feedback = this[StudentSubmissionsTable.feedback]
)

/**
 * DTO pour création (POST)
 */
@Serializable
data class StudentSubmissionCreateRequest(
    val studentId: Int,
    val courseId: String? = null,
    val sousCourseId: String? = null,
    val title: String,
    val fileUrl: String,
    val mimeType: String
)

/**
 * DTO pour mise à jour (PATCH)
 */
@Serializable
data class StudentSubmissionUpdateRequest(
    val title: String? = null,
    val grade: String? = null,
    val feedback: String? = null
)
