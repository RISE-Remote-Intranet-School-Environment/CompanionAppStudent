package be.ecam.server.services

import be.ecam.server.models.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.and

object CalendarService {


    // json event format
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

    fun seedCalendarEventsFromJson() {
        val resource = CalendarService::class.java.classLoader
            .getResource("files/ecam_calendar_events_2025_2026.json")
            ?: error("Resource 'files/ecam_calendar_events_2025_2026.json' not found in classpath")

        val text = resource.readText()
        val json = Json { ignoreUnknownKeys = true }
        val events = json.decodeFromString<List<CalendarEventJson>>(text)

        transaction {
            events.forEach { e ->
                val eventDate = e.date ?: e.start ?: "2025-01-01"

                CalendarEvent.new {
                    code = e.id
                    title = e.title
                    date = eventDate
                    startTime = "00:00"
                    endTime = "23:59"
                    room = null
                    sessionNumber = null
                    groupCode = e.annees_concernees?.firstOrNull()
                    ownerType = e.type.uppercase()
                    ownerRef = e.categorie ?: "GLOBAL"
                    course = null
                }
            }
        }
    }


    // json course schedule format
    @Serializable
    private data class CourseScheduleJson(
        val week: Int,
        val year_option: String,
        val group: Int,
        val series: List<String>,
        val date: String,
        val day_name: String,
        val start_time: String,
        val end_time: String,
        val course_code: String,
        val teachers: List<String>,
        val room: List<String>,
        val course_name: String
    )

    fun seedCourseScheduleFromJson() {
        val resource = CalendarService::class.java.classLoader
            .getResource("files/ecam_calendar_courses_schedule_2025.json")
            ?: error("Resource 'files/ecam_calendar_courses_schedule_2025.json' not found in classpath")

        val text = resource.readText()
        val json = Json { ignoreUnknownKeys = true }
        val records = json.decodeFromString<List<CourseScheduleJson>>(text)

        transaction {
            records.forEach { r ->
                CourseSchedule.new {
                    week = r.week
                    yearOption = r.year_option
                    groupNo = r.group
                    seriesJson = r.series.joinToString(",")    // stored as CSV
                    date = r.date
                    dayName = r.day_name
                    startTime = r.start_time
                    endTime = r.end_time
                    courseCode = r.course_code
                    teachersJson = r.teachers.joinToString(",")
                    roomsJson = r.room.joinToString(",")
                    courseName = r.course_name
                }
            }
        }
    }

    // read all events

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
     
    // read all course schedule
    fun getAllCourseSchedule(): List<CourseScheduleDTO> = transaction {
        CourseSchedule.all().map { it.toDTO() }
    }

    fun getScheduleForWeek(week: Int): List<CourseScheduleDTO> = transaction {
        CourseSchedule.find { CourseScheduleTable.week eq week }
            .map { it.toDTO() }
    }

    fun getScheduleForYear(year: String): List<CourseScheduleDTO> = transaction {
        CourseSchedule.find { CourseScheduleTable.yearOption eq year }
            .map { it.toDTO() }
    }

    fun getScheduleForYearAndGroup(year: String, group: Int): List<CourseScheduleDTO> =
        transaction {
            CourseSchedule.find {
                (CourseScheduleTable.yearOption eq year) and
                        (CourseScheduleTable.groupNo eq group)
            }.map { it.toDTO() }
        }

    fun getScheduleForCourse(code: String): List<CourseScheduleDTO> =
        transaction {
            CourseSchedule.find { CourseScheduleTable.courseCode eq code }
                .map { it.toDTO() }
        }

    
    // mapper functions

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

    private fun CourseSchedule.toDTO(): CourseScheduleDTO =
        CourseScheduleDTO(
            id = id.value,
            week = week,
            yearOption = yearOption,
            group = groupNo,
            series = seriesJson.split(","),
            date = date,
            dayName = dayName,
            startTime = startTime,
            endTime = endTime,
            courseCode = courseCode,
            courseName = courseName,
            teachers = teachersJson.split(","),
            rooms = roomsJson.split(",")
        )


    // CRUD for calendar events

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

}
