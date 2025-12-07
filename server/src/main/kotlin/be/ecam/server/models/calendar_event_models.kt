package be.ecam.server.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow


// Table pour les événements du calendrier
object CalendarEventsTable : IntIdTable("calendar_events") {
    val code = varchar("code", 50)
    val title = varchar("title", 255)
    val date = varchar("date", 20)          
    val startTime = varchar("start_time", 20)
    val endTime = varchar("end_time", 20)
    val groupCode = varchar("group_code", 50).nullable()
    val ownerType = varchar("owner_type", 50).nullable()
    val ownerRef = varchar("owner_ref", 255).nullable()
}

// DTO pour exposer les événements du calendrier au front
@Serializable
data class CalendarEventDTO(
    val id: Int,
    val code: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val groupCode: String?,
    val ownerType: String?,
    val ownerRef: String?
)

// mapper ResultRow -> DTO
fun ResultRow.toCalendarEventDTO() = CalendarEventDTO(
    id = this[CalendarEventsTable.id].value,
    code = this[CalendarEventsTable.code],
    title = this[CalendarEventsTable.title],
    date = this[CalendarEventsTable.date],
    startTime = this[CalendarEventsTable.startTime],
    endTime = this[CalendarEventsTable.endTime],
    groupCode = this[CalendarEventsTable.groupCode],
    ownerType = this[CalendarEventsTable.ownerType],
    ownerRef = this[CalendarEventsTable.ownerRef]
)

// DTO pour écriture (création et update complet)
@Serializable
data class CalendarEventWriteRequest(
    val code: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val groupCode: String? = null,
    val ownerType: String? = null,
    val ownerRef: String? = null
)
