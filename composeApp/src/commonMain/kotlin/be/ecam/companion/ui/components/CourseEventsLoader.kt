package be.ecam.companion.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import be.ecam.companion.ui.components.CourseEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.ExperimentalResourceApi
import companion.composeapp.generated.resources.Res

@Serializable
private data class RawCourseEvent(
    val date: String,
    val year_option: String,
    val series: List<String>,
    val course_code: String,
    val course_name: String,
    val start_time: String,
    val end_time: String,
    val teachers: List<String>,
    val room: List<String>
)

private object CourseEventsLoader {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): List<CourseEvent> {
        val bytes = Res.readBytes("files/ecam_calendar_courses_schedule_2025.json")
        val rawEvents = json.decodeFromString<List<RawCourseEvent>>(bytes.decodeToString())

        return rawEvents.map {
            CourseEvent(
                date = LocalDate.parse(it.date),
                yearOption = it.year_option,
                series = it.series,
                courseCode = it.course_code,
                courseName = it.course_name,
                startTime = it.start_time,
                endTime = it.end_time,
                teachers = it.teachers,
                rooms = it.room
            )
        }
    }
}

@Composable
fun rememberCourseEvents(): List<CourseEvent> {
    val state = produceState(initialValue = emptyList<CourseEvent>()) {
        value = CourseEventsLoader.load()
    }
    return state.value
}
