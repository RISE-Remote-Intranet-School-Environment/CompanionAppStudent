package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

@Serializable
enum class ResourceType {
    VIDEO, PDF, IMAGE, LINK, OTHER
}

object CourseResourcesTable : IntIdTable("course_resources") {

    // FK réelle -> prof
    val professor = reference("professor_id", ProfessorsTable)

    // Codes logiques (comme tes données)
    val courseId = varchar("course_id", 50).nullable()
    val sousCourseId = varchar("sous_course_id", 50).nullable()

    val title = varchar("title", 255)
    val description = text("description").nullable()

    val resourceType = enumerationByName("resource_type", 20, ResourceType::class)
    val resourceUrl = varchar("resource_url", 1024)

    val thumbnailUrl = varchar("thumbnail_url", 1024).nullable()
    val durationSeconds = integer("duration_seconds").nullable()

    val uploadedAt = long("uploaded_at")
}

@Serializable
data class CourseResourceDTO(
    val id: Int,
    val professorId: Int,
    val courseId: String?,
    val sousCourseId: String?,
    val title: String,
    val description: String?,
    val resourceType: ResourceType,
    val resourceUrl: String,
    val thumbnailUrl: String?,
    val durationSeconds: Int?,
    val uploadedAt: Long
)

fun ResultRow.toCourseResourceDTO() = CourseResourceDTO(
    id = this[CourseResourcesTable.id].value,
    professorId = this[CourseResourcesTable.professor].value,
    courseId = this[CourseResourcesTable.courseId],
    sousCourseId = this[CourseResourcesTable.sousCourseId],
    title = this[CourseResourcesTable.title],
    description = this[CourseResourcesTable.description],
    resourceType = this[CourseResourcesTable.resourceType],
    resourceUrl = this[CourseResourcesTable.resourceUrl],
    thumbnailUrl = this[CourseResourcesTable.thumbnailUrl],
    durationSeconds = this[CourseResourcesTable.durationSeconds],
    uploadedAt = this[CourseResourcesTable.uploadedAt]
)

@Serializable
data class CourseResourceCreateRequest(
    val professorId: Int,
    val courseId: String? = null,
    val sousCourseId: String? = null,
    val title: String,
    val description: String? = null,
    val resourceType: ResourceType,
    val resourceUrl: String,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null
)

@Serializable
data class CourseResourceUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val resourceType: ResourceType? = null,
    val resourceUrl: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null
)
