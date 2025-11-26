package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

// table
object CalendarEventsTable : IntIdTable("calendar_events") {
    val code = varchar("code", 50)
    val title = varchar("title", 255)
    val date = varchar("date", 10)
    val startTime = varchar("start_time", 5)
    val endTime = varchar("end_time", 5)
    val room = varchar("room", 20).nullable()
    val sessionNumber = integer("session_number").nullable()
    val groupCode = varchar("group_code", 20).nullable()

    val ownerType = varchar("owner_type", 20)
    val ownerRef = varchar("owner_ref", 50)

    val course = reference("course_id", CourseTable).nullable()
}


// entity
class CalendarEvent(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CalendarEvent>(CalendarEventsTable)

    var code by CalendarEventsTable.code
    var title by CalendarEventsTable.title
    var date by CalendarEventsTable.date
    var startTime by CalendarEventsTable.startTime
    var endTime by CalendarEventsTable.endTime
    var room by CalendarEventsTable.room
    var sessionNumber by CalendarEventsTable.sessionNumber
    var groupCode by CalendarEventsTable.groupCode

    var ownerType by CalendarEventsTable.ownerType
    var ownerRef by CalendarEventsTable.ownerRef

    var course by Course optionalReferencedOn CalendarEventsTable.course
}


// dto read
@Serializable
data class CalendarEventDTO(
    val id: Int,
    val code: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val room: String?,
    val sessionNumber: Int?,
    val groupCode: String?,
    val ownerType: String,
    val ownerRef: String,
    val courseCode: String?
)

// dto write
@Serializable
data class CalendarEventWriteRequest(
    val code: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val room: String? = null,
    val sessionNumber: Int? = null,
    val groupCode: String? = null,
    val ownerType: String,
    val ownerRef: String,
    val courseId: Int? = null
)



// table
object CourseScheduleTable : IntIdTable("course_schedule") {
    val week = integer("week")
    val yearOption = varchar("year_option", 20)
    val groupNo = integer("group_no")

    val seriesJson = text("series_json")
    val date = varchar("date", 10)
    val dayName = varchar("day_name", 20)
    val startTime = varchar("start_time", 5)
    val endTime = varchar("end_time", 5)

    val courseCode = varchar("course_code", 20)
    val teachersJson = text("teachers_json")
    val roomsJson = text("rooms_json")
    val courseName = varchar("course_name", 255)
}

class CourseSchedule(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CourseSchedule>(CourseScheduleTable)

    var week by CourseScheduleTable.week
    var yearOption by CourseScheduleTable.yearOption
    var groupNo by CourseScheduleTable.groupNo

    var seriesJson by CourseScheduleTable.seriesJson
    var date by CourseScheduleTable.date
    var dayName by CourseScheduleTable.dayName
    var startTime by CourseScheduleTable.startTime
    var endTime by CourseScheduleTable.endTime

    var courseCode by CourseScheduleTable.courseCode
    var teachersJson by CourseScheduleTable.teachersJson
    var roomsJson by CourseScheduleTable.roomsJson
    var courseName by CourseScheduleTable.courseName
}

@Serializable
data class CourseScheduleDTO(
    val id: Int,
    val week: Int,
    val yearOption: String,
    val group: Int,
    val series: List<String>,
    val date: String,
    val dayName: String,
    val startTime: String,
    val endTime: String,
    val courseCode: String,
    val courseName: String,
    val teachers: List<String>,
    val rooms: List<String>
)
