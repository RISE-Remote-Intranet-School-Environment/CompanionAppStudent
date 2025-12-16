package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

// Table pour les horaires de cours
object CourseScheduleTable : IntIdTable("course_schedule") {
    val week = integer("week")
    val yearOptionId = varchar("year_option_id", 50)
    val groupNo = varchar("group_no", 50)
    val seriesJson = text("series_json").nullable()
    val date = varchar("date", 20)
    val dayName = varchar("day_name", 20)
    val startTime = varchar("start_time", 20)
    val endTime = varchar("end_time", 20)
    val courseRaccourciId = varchar("course_raccourci_id", 50)
    val title = varchar("title", 255)
    val teachersJson = text("teachers_json").nullable()
    val roomIds = text("room_ids").nullable()
    val sousCourseId = varchar("sous_course_id", 50).nullable()
}


// DTO pour exposer les horaires de cours au front
@Serializable
data class CourseScheduleDTO(
    val id: Int,
    val week: Int,
    val yearOptionId: String,
    val groupNo: String,
    val seriesJson: String?,
    val date: String,
    val dayName: String,
    val startTime: String,
    val endTime: String,
    val courseRaccourciId: String,
    val title: String,
    val teachersJson: String?,
    val roomIds: String?,
    val sousCourseId: String?
)

// mapper ResultRow -> DTO
fun ResultRow.toCourseScheduleDTO() = CourseScheduleDTO(
    id = this[CourseScheduleTable.id].value,
    week = this[CourseScheduleTable.week],
    yearOptionId = this[CourseScheduleTable.yearOptionId],
    groupNo = this[CourseScheduleTable.groupNo],
    seriesJson = this[CourseScheduleTable.seriesJson],
    date = this[CourseScheduleTable.date],
    dayName = this[CourseScheduleTable.dayName],
    startTime = this[CourseScheduleTable.startTime],
    endTime = this[CourseScheduleTable.endTime],
    courseRaccourciId = this[CourseScheduleTable.courseRaccourciId],
    title = this[CourseScheduleTable.title],
    teachersJson = this[CourseScheduleTable.teachersJson],
    roomIds = this[CourseScheduleTable.roomIds],
    sousCourseId = this[CourseScheduleTable.sousCourseId]
)

// DTO pour écriture (création et update complet)
@Serializable
data class CourseScheduleWriteRequest(
    val week: Int,
    val yearOptionId: String,
    val groupNo: String,
    val seriesJson: String? = null,
    val date: String,
    val dayName: String,
    val startTime: String,
    val endTime: String,
    val courseRaccourciId: String,
    val title: String,
    val teachersJson: String? = null,
    val roomIds: String? = null,
    val sousCourseId: String? = null
)

