package be.ecam.companion.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import be.ecam.companion.data.CalendarRepository
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.buildBaseUrl
import io.ktor.client.HttpClient
import org.koin.compose.koinInject
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
        value = runCatching { repository.fetchCourseEvents() }.getOrElse { emptyList() }
    }
    return state.value
}
