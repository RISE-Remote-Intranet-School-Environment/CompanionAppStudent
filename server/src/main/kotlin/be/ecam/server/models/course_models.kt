package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table pour les cours
object CoursesTable : IntIdTable("courses") {
    val courseId = varchar("course_id", 50)
    val courseRaccourciId = varchar("course_raccourci_id", 50)
    val title = varchar("title", 255)
    val credits = integer("credits")
    val periods = integer("periods")
    val detailsUrl = varchar("details_url", 512).nullable()
    val mandatory = bool("mandatory")
    val blocId = integer("bloc_id").nullable()        // FK logique vers blocs.id
    val formationId = integer("formation_id").nullable() // FK logique vers formations.id
    val language = varchar("language", 10)
}

// DTO pour exposer les cours au front
@Serializable
data class CourseDTO(
    val id: Int,
    val courseId: String,
    val courseRaccourciId: String,
    val title: String,
    val credits: Int,
    val periods: Int,
    val detailsUrl: String?,
    val mandatory: Boolean,
    val blocId: Int?,
    val formationId: Int?,
    val language: String
)

// mapper ResultRow -> DTO
fun ResultRow.toCourseDTO() = CourseDTO(
    id = this[CoursesTable.id].value,
    courseId = this[CoursesTable.courseId],
    courseRaccourciId = this[CoursesTable.courseRaccourciId],
    title = this[CoursesTable.title],
    credits = this[CoursesTable.credits],
    periods = this[CoursesTable.periods],
    detailsUrl = this[CoursesTable.detailsUrl],
    mandatory = this[CoursesTable.mandatory],
    blocId = this[CoursesTable.blocId],
    formationId = this[CoursesTable.formationId],
    language = this[CoursesTable.language]
)

// DTO pour écriture (création et update complet)
@Serializable
data class CourseWriteRequest(
    val courseId: String,
    val courseRaccourciId: String,
    val title: String,
    val credits: Int,
    val periods: Int,
    val detailsUrl: String? = null,
    val mandatory: Boolean,
    val blocId: Int? = null,
    val formationId: Int? = null,
    val language: String
)
