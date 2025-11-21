package be.ecam.server.services

import be.ecam.server.models.*
import org.jetbrains.exposed.sql.transactions.transaction

object CalendarService {

    // Retourne tous les events (pour debug / vue globale)
    fun getAllEvents(): List<CalendarEventDTO> = transaction {
        CalendarEvent.all().map { it.toDTO() }
    }

    // Events pour un groupe d’étudiants (ex: "2BA-s3")
    fun getEventsForGroup(groupCode: String): List<CalendarEventDTO> = transaction {
        CalendarEvent.find { CalendarEventsTable.groupCode eq groupCode }
            .map { it.toDTO() }
    }

    // Events pour un prof (ex: "DLH"), plus tard
    fun getEventsForOwner(ownerRef: String): List<CalendarEventDTO> = transaction {
        CalendarEvent.find { CalendarEventsTable.ownerRef eq ownerRef }
            .map { it.toDTO() }
    }

    // Extension pour convertir Entity -> DTO
    private fun CalendarEvent.toDTO(): CalendarEventDTO =
        CalendarEventDTO(
            id = this.id.value,
            code = this.code,
            title = this.title,
            date = this.date,
            startTime = this.startTime,
            endTime = this.endTime,
            room = this.room,
            sessionNumber = this.sessionNumber,
            groupCode = this.groupCode,
            ownerType = this.ownerType,
            ownerRef = this.ownerRef,
            courseCode = this.course?.code
        )
}
