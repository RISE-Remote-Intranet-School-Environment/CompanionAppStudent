package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

object CourseResourcesTable : IntIdTable("course_resources") {
    val professor = integer("professor_id").references(ProfessorsTable.id).nullable()
    val courseId = varchar("course_id", 50).nullable()
    val sousCourseId = varchar("sous_course_id", 50).nullable()
    val title = varchar("title", 255)
    // DB columns are resource_type/resource_url in app.db
    val type = varchar("resource_type", 50).default("link") // link, file, pdf, etc.
    val url = text("resource_url")
    val uploadedAt = long("uploaded_at").default(System.currentTimeMillis())
}

@Serializable
data class CourseResourceDTO(
    val id: Int,
    val professorId: Int?,
    val professorCode: String? = null,
    val courseId: String?,
    val sousCourseId: String?,
    val title: String,
    val type: String,
    val url: String,
    val uploadedAt: Long
)

fun ResultRow.toCourseResourceDTO() = CourseResourceDTO(
    id = this[CourseResourcesTable.id].value,
    professorId = this[CourseResourcesTable.professor],
    professorCode = null,
    courseId = this[CourseResourcesTable.courseId],
    sousCourseId = this[CourseResourcesTable.sousCourseId],
    title = this[CourseResourcesTable.title],
    type = this[CourseResourcesTable.type],
    url = this[CourseResourcesTable.url],
    uploadedAt = this[CourseResourcesTable.uploadedAt]
)

fun ResultRow.toCourseResourceDTOWithProfessorCode() = CourseResourceDTO(
    id = this[CourseResourcesTable.id].value,
    professorId = this[CourseResourcesTable.professor],
    professorCode = this.getOrNull(ProfessorsTable.professorId),
    courseId = this[CourseResourcesTable.courseId],
    sousCourseId = this[CourseResourcesTable.sousCourseId],
    title = this[CourseResourcesTable.title],
    type = this[CourseResourcesTable.type],
    url = this[CourseResourcesTable.url],
    uploadedAt = this[CourseResourcesTable.uploadedAt]
)

@Serializable
data class CourseResourceCreateRequest(
    val professorId: Int? = null,
    val courseId: String? = null,
    val sousCourseId: String? = null,
    val title: String,
    val type: String = "link",
    val url: String
)

@Serializable
data class CourseResourceUpdateRequest(
    val title: String? = null,
    val type: String? = null,
    val url: String? = null
)
