package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table pour les sous-cours
object SousCoursesTable : IntIdTable("sous_course") {
    val sousCourseId = varchar("sous_course_id", 50)
    val courseId = varchar("course_id", 50)
    val title = varchar("title", 255)
    val hoursQ1 = integer("hours_Q1")
    val hoursQ2 = integer("hours_Q2")
    val teachersIds = text("teachers_ids")       // JSON/liste
    val language = varchar("language", 10)
}

// DTO pour exposer les sous-cours au front
@Serializable
data class SousCourseDTO(
    val id: Int,
    val sousCourseId: String,
    val courseId: String,
    val title: String,
    val hoursQ1: Int,
    val hoursQ2: Int,
    val teachersIds: String,
    val language: String
)

// mapper ResultRow -> DTO
fun ResultRow.toSousCourseDTO() = SousCourseDTO(
    id = this[SousCoursesTable.id].value,
    sousCourseId = this[SousCoursesTable.sousCourseId],
    courseId = this[SousCoursesTable.courseId],
    title = this[SousCoursesTable.title],
    hoursQ1 = this[SousCoursesTable.hoursQ1],
    hoursQ2 = this[SousCoursesTable.hoursQ2],
    teachersIds = this[SousCoursesTable.teachersIds],
    language = this[SousCoursesTable.language]
)

// DTO pour écriture (création et update complet)
@Serializable
data class SousCourseWriteRequest(
    val sousCourseId: String,
    val courseId: String,
    val title: String,
    val hoursQ1: Int,
    val hoursQ2: Int,
    val teachersIds: String,
    val language: String
)
