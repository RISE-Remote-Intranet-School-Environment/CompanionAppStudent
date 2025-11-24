package be.ecam.server.services

import be.ecam.server.models.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object CalendarService {

    // ============================================================
    // JSON pour ton fichier ecam_calendar_events_2025_2026.json
    // ============================================================

    @Serializable
    private data class CalendarEventJson(
        val id: String,
        val type: String,
        val start: String? = null,
        val end: String? = null,
        val date: String? = null,
        val description: String? = null,
        val annees_concernees: List<String>? = null,
        val categorie: String? = null,
        val title: String
    )

    /**
     * Importe les événements depuis resources/files/ecam_calendar_events_2025_2026.json
     */
    fun seedCalendarEventsFromJson() {
        val resource = CalendarService::class.java.classLoader
            .getResource("files/ecam_calendar_events_2025_2026.json")
            ?: error("Resource 'files/ecam_calendar_events_2025_2026.json' introuvable dans le classpath")

        val text = resource.readText()
        val json = Json { ignoreUnknownKeys = true }

        // TON JSON → une LISTE d'objets, pas un objet global
        val events = json.decodeFromString<List<CalendarEventJson>>(text)

        transaction {
            events.forEach { e ->

                // 1) Date (si "date" absent, on prend "start")
                val eventDate = e.date ?: e.start ?: "2025-01-01"

                // 2) Pas d'heures dans ton fichier → valeurs par défaut
                val startTime = "00:00"
                val endTime = "23:59"

                // 3) Groupe : première année concernée
                val groupCode = e.annees_concernees?.firstOrNull()

                // 4) Owner
                val ownerType = e.type.uppercase()              // evenement
                val ownerRef = e.categorie ?: "GLOBAL"

                // 5) AUCUN cours lié → course = null
                CalendarEvent.new {
                    code = e.id
                    title = e.title
                    date = eventDate
                    this.startTime = startTime
                    this.endTime = endTime
                    room = null
                    sessionNumber = null
                    this.groupCode = groupCode
                    this.ownerType = ownerType
                    this.ownerRef = ownerRef
                    course = null
                }
            }
        }
    }

    // ============================================================
    // LECTURE
    // ============================================================

    fun getAllEvents(): List<CalendarEventDTO> = transaction {
        CalendarEvent.all().map { it.toDTO() }
    }

    fun getEventsForGroup(groupCode: String): List<CalendarEventDTO> = transaction {
        CalendarEvent.find { CalendarEventsTable.groupCode eq groupCode }
            .map { it.toDTO() }
    }

    fun getEventsForOwner(ownerRef: String): List<CalendarEventDTO> = transaction {
        CalendarEvent.find { CalendarEventsTable.ownerRef eq ownerRef }
            .map { it.toDTO() }
    }

    // ============================================================
    // CRUD ADMIN
    // ============================================================

    fun createEvent(req: CalendarEventWriteRequest): CalendarEventDTO = transaction {
        val course = req.courseId?.let { Course.findById(it) }

        CalendarEvent.new {
            code = req.code
            title = req.title
            date = req.date
            startTime = req.startTime
            endTime = req.endTime
            room = req.room
            sessionNumber = req.sessionNumber
            groupCode = req.groupCode
            ownerType = req.ownerType
            ownerRef = req.ownerRef
            this.course = course
        }.toDTO()
    }

    fun updateEvent(id: Int, req: CalendarEventWriteRequest): CalendarEventDTO? = transaction {
        val event = CalendarEvent.findById(id) ?: return@transaction null
        val course = req.courseId?.let { Course.findById(it) }

        event.apply {
            code = req.code
            title = req.title
            date = req.date
            startTime = req.startTime
            endTime = req.endTime
            room = req.room
            sessionNumber = req.sessionNumber
            groupCode = req.groupCode
            ownerType = req.ownerType
            ownerRef = req.ownerRef
            this.course = course
        }.toDTO()
    }

    fun deleteEvent(id: Int): Boolean = transaction {
        val event = CalendarEvent.findById(id) ?: return@transaction false
        event.delete()
        true
    }

    // ============================================================
    // Mapping Entity -> DTO
    // ============================================================

    private fun CalendarEvent.toDTO(): CalendarEventDTO =
        CalendarEventDTO(
            id = id.value,
            code = code,
            title = title,
            date = date,
            startTime = startTime,
            endTime = endTime,
            room = room,
            sessionNumber = sessionNumber,
            groupCode = groupCode,
            ownerType = ownerType,
            ownerRef = ownerRef,
            courseCode = course?.code
        )
}
