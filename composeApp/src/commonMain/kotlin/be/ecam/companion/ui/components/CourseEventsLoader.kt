package be.ecam.companion.ui.components

import androidx.compose.runtime.*
import be.ecam.companion.data.CalendarRepository
import be.ecam.companion.data.CourseScheduleEvent
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Composable helper pour charger les événements de cours
 */
@Composable
fun rememberCourseEvents(
    calendarRepository: CalendarRepository = koinInject(),
    authToken: String?,
    yearOptionId: String? = null,
    seriesId: String? = null
): State<List<CourseScheduleEvent>> {
    val events = remember { mutableStateOf<List<CourseScheduleEvent>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(authToken, yearOptionId, seriesId) {
        scope.launch {
            events.value = calendarRepository.getCourseSchedule(
                token = authToken,
                yearOptionId = yearOptionId,
                seriesId = seriesId
            )
        }
    }

    return events
}
