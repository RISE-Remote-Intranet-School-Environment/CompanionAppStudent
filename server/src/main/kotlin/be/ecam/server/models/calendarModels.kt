package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

// TABLE des événements du calendrier
object CalendarEventsTable : IntIdTable("calendar_events") {
    val code = varchar("code", 50)               // code de session : "EO2L-L2-2BA-A"
    val title = varchar("title", 255)            // "Laboratoire d’électronique"
    val date = varchar("date", 10)               // "2025-11-28"
    val startTime = varchar("start_time", 5)     // "12:45"
    val endTime = varchar("end_time", 5)         // "16:15"
    val room = varchar("room", 20).nullable()    // "1F04"
    val sessionNumber = integer("session_number").nullable()   // 2
    val groupCode = varchar("group_code", 20).nullable()       // "2BA-s3"

    // pour plus tard : savoir pour qui est l'event
    val ownerType = varchar("owner_type", 20)    // "TEACHER" ou "STUDENT"
    val ownerRef = varchar("owner_ref", 50)      // ex : "DLH" ou "2BA-s3"

    // on relie optionnellement à un cours existant
    val course = reference("course_id", CourseTable).nullable()
}

// ENTITY Exposed
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

// DTO exposé à l’API (simple, aligné avec ton JSON)
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
    val courseCode: String?        // récupéré depuis Course.code si lié
)
