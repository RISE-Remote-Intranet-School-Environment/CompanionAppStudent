package be.ecam.companion.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import be.ecam.companion.data.CalendarRepository
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.buildBaseUrl
import io.ktor.client.HttpClient
import org.koin.compose.koinInject
import companion.composeapp.generated.resources.Res
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Composable
fun rememberCourseEvents(authToken: String? = null): List<CourseEvent> {
    val httpClient = koinInject<HttpClient>()
    val settingsRepo = koinInject<SettingsRepository>()
    val host = settingsRepo.getServerHost()
    val port = settingsRepo.getServerPort()
    val repository = CalendarRepository(
        client = httpClient,
        baseUrlProvider = { buildBaseUrl(host, port) },
        authTokenProvider = { authToken }
    )

    val state = produceState(initialValue = emptyList<CourseEvent>(), host, port, authToken) {
        val remote = runCatching { repository.fetchCourseEvents() }.getOrElse { emptyList() }
        value = if (remote.isNotEmpty()) remote else loadLocalFallback()
    }
    return state.value
}

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

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadLocalFallback(): List<CourseEvent> {
    return try {
        val bytes = Res.readBytes("files/ecam_calendar_courses_schedule_2025.json")
        val rawEvents = Json { ignoreUnknownKeys = true }.decodeFromString<List<RawCourseEvent>>(bytes.decodeToString())
        rawEvents.map {
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
    } catch (_: Throwable) {
        emptyList()
    }
}
