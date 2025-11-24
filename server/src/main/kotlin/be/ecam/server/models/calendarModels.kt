package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

// ---------------------------
// TABLE
// ---------------------------
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

// ---------------------------
// ENTITY
// ---------------------------
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

// ---------------------------
// DTO READ
// ---------------------------
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

// ---------------------------
// DTO WRITE (pour POST/PUT futur)
// ---------------------------
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
