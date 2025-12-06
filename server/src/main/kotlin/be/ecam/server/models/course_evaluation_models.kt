package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table pour les évaluations de cours
object CourseEvaluationTable : IntIdTable("course_evaluation") {
    val evaluatedActivityId = varchar("evaluated_activity_id", 50)
    val courseId = varchar("course_id", 50)
    val weight = double("weight")
    val typeQ1 = varchar("type_Q1", 255).nullable()
    val typeQ2 = varchar("type_Q2", 255).nullable()
    val typeQ3 = varchar("type_Q3", 255).nullable()
    val sousCourseIds = text("sous_course_ids").nullable()
    val teachersIds = text("teachers_ids").nullable()
}

// DTO pour exposer les évaluations de cours au front
@Serializable
data class CourseEvaluationDTO(
    val id: Int,
    val evaluatedActivityId: String,
    val courseId: String,
    val weight: Double,
    val typeQ1: String?,
    val typeQ2: String?,
    val typeQ3: String?,
    val sousCourseIds: String?,
    val teachersIds: String?
)

// mapper ResultRow -> DTO
fun ResultRow.toCourseEvaluationDTO() = CourseEvaluationDTO(
    id = this[CourseEvaluationTable.id].value,
    evaluatedActivityId = this[CourseEvaluationTable.evaluatedActivityId],
    courseId = this[CourseEvaluationTable.courseId],
    weight = this[CourseEvaluationTable.weight],
    typeQ1 = this[CourseEvaluationTable.typeQ1],
    typeQ2 = this[CourseEvaluationTable.typeQ2],
    typeQ3 = this[CourseEvaluationTable.typeQ3],
    sousCourseIds = this[CourseEvaluationTable.sousCourseIds],
    teachersIds = this[CourseEvaluationTable.teachersIds]
)

// DTO pour écriture (création et update complet)
@Serializable
data class CourseEvaluationWriteRequest(
    val evaluatedActivityId: String,
    val courseId: String,
    val weight: Double,
    val typeQ1: String? = null,
    val typeQ2: String? = null,
    val typeQ3: String? = null,
    val sousCourseIds: String? = null,
    val teachersIds: String? = null
)

