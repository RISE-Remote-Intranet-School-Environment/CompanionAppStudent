package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable


// table 
object CourseDetailsTable : IntIdTable("course_details") {
    val courseCode = varchar("course_id", 255).uniqueIndex()
    val sousCourseId = varchar("sous_course_id", 255).nullable()
    val responsable = varchar("responsable", 255).nullable()
    val teachersRaw = text("teachers_raw_id").nullable()
    val formationIds = text("formation_ids").nullable()
    val periods = text("periods").nullable()
    val hoursQ1 = text("hours_q1").nullable()
    val hoursQ2 = text("hours_q2").nullable()
    val contribution = text("contribution").nullable()
    val learningOutcomes = text("learning_outcomes").nullable()
    val content = text("content").nullable()
    val teachingMethods = text("teaching_methods").nullable()
    val evaluationMethods = text("evaluation_methods").nullable()
    val courseMaterial = text("course_material").nullable()
    val bibliography = text("bibliography").nullable()
    val blocId = varchar("bloc_id", 255).nullable()
}

// entity
class CourseDetails(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CourseDetails>(CourseDetailsTable)

    var courseCode by CourseDetailsTable.courseCode
    var sousCourseId by CourseDetailsTable.sousCourseId
    var responsable by CourseDetailsTable.responsable
    var teachersRaw by CourseDetailsTable.teachersRaw
    var formationIds by CourseDetailsTable.formationIds
    var periods by CourseDetailsTable.periods
    var hoursQ1 by CourseDetailsTable.hoursQ1
    var hoursQ2 by CourseDetailsTable.hoursQ2
    var contribution by CourseDetailsTable.contribution
    var learningOutcomes by CourseDetailsTable.learningOutcomes
    var content by CourseDetailsTable.content
    var teachingMethods by CourseDetailsTable.teachingMethods
    var evaluationMethods by CourseDetailsTable.evaluationMethods
    var courseMaterial by CourseDetailsTable.courseMaterial
    var bibliography by CourseDetailsTable.bibliography
    var blocId by CourseDetailsTable.blocId

    fun teachersList(): List<String> =
        teachersRaw
            ?.split(";")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
}

// dto read
@Serializable
data class CourseDetailsDTO(
    val id: Int,
    val courseId: Int?,
    val courseCode: String,
    val titre: String?,
    val responsable: String?,
    val teachers: List<String>,
    val contribution: String?,
    val learningOutcomes: String?,
    val content: String?,
    val teachingMethods: String?,
    val evaluationMethods: String?,
    val courseMaterial: String?,
    val bibliography: String?
)
