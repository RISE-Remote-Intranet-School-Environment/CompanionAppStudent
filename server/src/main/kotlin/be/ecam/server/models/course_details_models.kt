package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table pour les détails des cours
object CourseDetailsTable : IntIdTable("course_details") {
    val courseId = varchar("course_id", 50)
    val responsable = varchar("responsable", 255).nullable()
    val sousCourseId = varchar("sous_course_id", 50).nullable()
    val teachersRawId = varchar("teachers_raw_id", 255).nullable()
    val formationIds = text("formation_ids").nullable()
    val periods = varchar("periods", 50).nullable()
    val hoursQ1 = varchar("hours_q1", 50).nullable()
    val hoursQ2 = varchar("hours_q2", 50).nullable()
    val contribution = text("contribution").nullable()
    val learningOutcomes = text("learning_outcomes").nullable()
    val content = text("content").nullable()
    val teachingMethods = text("teaching_methods").nullable()
    val evaluationMethods = text("evaluation_methods").nullable()
    val courseMaterial = text("course_material").nullable()
    val bibliography = text("bibliography").nullable()
    val blocId = varchar("bloc_id", 255).nullable()
}

// DTO pour exposer les détails des cours au front
@Serializable
data class CourseDetailsDTO(
    val id: Int,
    val courseId: String,
    val responsable: String?,
    val sousCourseId: String?,
    val teachersRawId: String?,
    val formationIds: String?,
    val periods: String?,
    val hoursQ1: String?,
    val hoursQ2: String?,
    val contribution: String?,
    val learningOutcomes: String?,
    val content: String?,
    val teachingMethods: String?,
    val evaluationMethods: String?,
    val courseMaterial: String?,
    val bibliography: String?,
    val blocId: String?
)

// mapper ResultRow -> DTO
fun ResultRow.toCourseDetailsDTO() = CourseDetailsDTO(
    id = this[CourseDetailsTable.id].value,
    courseId = this[CourseDetailsTable.courseId],
    responsable = this[CourseDetailsTable.responsable],
    sousCourseId = this[CourseDetailsTable.sousCourseId],
    teachersRawId = this[CourseDetailsTable.teachersRawId],
    formationIds = this[CourseDetailsTable.formationIds],
    periods = this[CourseDetailsTable.periods],
    hoursQ1 = this[CourseDetailsTable.hoursQ1],
    hoursQ2 = this[CourseDetailsTable.hoursQ2],
    contribution = this[CourseDetailsTable.contribution],
    learningOutcomes = this[CourseDetailsTable.learningOutcomes],
    content = this[CourseDetailsTable.content],
    teachingMethods = this[CourseDetailsTable.teachingMethods],
    evaluationMethods = this[CourseDetailsTable.evaluationMethods],
    courseMaterial = this[CourseDetailsTable.courseMaterial],
    bibliography = this[CourseDetailsTable.bibliography],
    blocId = this[CourseDetailsTable.blocId]
)

// DTO pour ecriture (création et update complet)
@Serializable
data class CourseDetailsWriteRequest(
    val courseId: String,
    val responsable: String? = null,
    val sousCourseId: String? = null,
    val teachersRawId: String? = null,
    val formationIds: String? = null,
    val periods: String? = null,
    val hoursQ1: String? = null,
    val hoursQ2: String? = null,
    val contribution: String? = null,
    val learningOutcomes: String? = null,
    val content: String? = null,
    val teachingMethods: String? = null,
    val evaluationMethods: String? = null,
    val courseMaterial: String? = null,
    val bibliography: String? = null,
    val blocId: String? = null
)

