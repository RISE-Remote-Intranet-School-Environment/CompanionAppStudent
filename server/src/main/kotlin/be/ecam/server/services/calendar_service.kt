package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CalendarService {

    fun getAllEvents(): List<CalendarEventDTO> = transaction {
        CalendarEventsTable
            .selectAll()
            .map { it.toCalendarEventDTO() }
    }

    fun getEventById(id: Int): CalendarEventDTO? = transaction {
        CalendarEventsTable
            .selectAll()
            .where { CalendarEventsTable.id eq id }
            .singleOrNull()
            ?.toCalendarEventDTO()
    }

    fun getEventsByDate(date: String): List<CalendarEventDTO> = transaction {
        CalendarEventsTable
            .selectAll()
            .where { CalendarEventsTable.date eq date }
            .map { it.toCalendarEventDTO() }
    }

    fun getEventsByGroup(group: String): List<CalendarEventDTO> = transaction {
        CalendarEventsTable
            .selectAll()
            .where { CalendarEventsTable.groupCode eq group }
            .map { it.toCalendarEventDTO() }
    }

    fun createEvent(req: CalendarEventWriteRequest): CalendarEventDTO = transaction {
        val newId = CalendarEventsTable.insertAndGetId { row ->
            row[CalendarEventsTable.code] = req.code
            row[CalendarEventsTable.title] = req.title
            row[CalendarEventsTable.date] = req.date
            row[CalendarEventsTable.startTime] = req.startTime
            row[CalendarEventsTable.endTime] = req.endTime
            row[CalendarEventsTable.groupCode] = req.groupCode
            row[CalendarEventsTable.ownerType] = req.ownerType
            row[CalendarEventsTable.ownerRef] = req.ownerRef
        }

        CalendarEventsTable
            .selectAll()
            .where { CalendarEventsTable.id eq newId }
            .single()
            .toCalendarEventDTO()
    }

    fun updateEvent(id: Int, req: CalendarEventWriteRequest): CalendarEventDTO? = transaction {
        val updated = CalendarEventsTable.update({ CalendarEventsTable.id eq id }) { row ->
            row[CalendarEventsTable.code] = req.code
            row[CalendarEventsTable.title] = req.title
            row[CalendarEventsTable.date] = req.date
            row[CalendarEventsTable.startTime] = req.startTime
            row[CalendarEventsTable.endTime] = req.endTime
            row[CalendarEventsTable.groupCode] = req.groupCode
            row[CalendarEventsTable.ownerType] = req.ownerType
            row[CalendarEventsTable.ownerRef] = req.ownerRef
        }

        if (updated == 0) return@transaction null

        CalendarEventsTable
            .selectAll()
            .where { CalendarEventsTable.id eq id }
            .singleOrNull()
            ?.toCalendarEventDTO()
    }

    fun deleteEvent(id: Int): Boolean = transaction {
        CalendarEventsTable.deleteWhere { CalendarEventsTable.id eq id } > 0
    }
}
