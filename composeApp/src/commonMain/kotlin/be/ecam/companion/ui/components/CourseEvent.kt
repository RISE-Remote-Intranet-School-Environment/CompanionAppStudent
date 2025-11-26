package be.ecam.companion.ui.components

import be.ecam.companion.ui.screens.CalendarEvent
import be.ecam.companion.ui.screens.CalendarEventCategory
import kotlinx.datetime.LocalDate

data class CourseEvent(
    val date: LocalDate,
    val yearOption: String,
    val series: List<String>,
    val courseCode: String,
    val courseName: String,
    val startTime: String,
    val endTime: String,
    val teachers: List<String>,
    val rooms: List<String>
)

fun CourseEvent.toCalendarEvent(): CalendarEvent {
    return CalendarEvent(
        id = "${courseCode}_${date}",
        title = "$courseName ($courseCode)",
        description = "${startTime} - ${endTime}\nProf: ${teachers.joinToString()}\nSalle: ${rooms.joinToString()}",
        category = CalendarEventCategory.Course,
        date = date,
        years = listOf(yearOption)
    )
}
