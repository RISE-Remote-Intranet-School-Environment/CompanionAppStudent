package be.ecam.companion.ui.components

import be.ecam.companion.ui.screens.CalendarEvent
import kotlinx.datetime.LocalDate

fun List<CourseEvent>.toCalendarEventsByDate(): Map<LocalDate, List<CalendarEvent>> {
    return this
        .map { it.toCalendarEvent() }
        .groupBy { it.date }
}
