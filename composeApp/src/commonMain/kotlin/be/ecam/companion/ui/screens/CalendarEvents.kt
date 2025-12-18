package be.ecam.companion.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import be.ecam.companion.data.CalendarRepository
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.buildBaseUrl
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String,
    val category: CalendarEventCategory,
    val date: LocalDate,
    val years: List<String>
)

enum class CalendarEventCategory(val key: String, val color: Color) {
    Rentree("rentree", Color(0xFF7B57C2)),
    Conges("conges", Color(0xFF4CAF50)),
    Examens("examens", Color(0xFFEF6C00)),
    Stage("stage", Color(0xFF00897B)),
    Autre("autre", Color(0xFF6D6D6D)),
    Tfe("tfe", Color(0xFF5C6BC0)),
    Remote("remote", Color(0xFF607D8B)),
    Course("course", Color(0xFF1976D2)),
    Unknown("unknown", Color(0xFF9E9E9E));

    companion object {
        fun fromKey(key: String?): CalendarEventCategory =
            when (key?.lowercase()) {
                Rentree.key -> Rentree
                Conges.key -> Conges
                Examens.key -> Examens
                Stage.key -> Stage
                Autre.key -> Autre
                Tfe.key -> Tfe
                Remote.key -> Remote
                Course.key -> Course
                else -> Unknown
            }
    }
}

@Composable
fun rememberCalendarEventsByDate(authToken: String? = null): Map<LocalDate, List<CalendarEvent>> {
    val httpClient = koinInject<HttpClient>()
    val settingsRepo = koinInject<SettingsRepository>()
    val host = settingsRepo.getServerHost()
    val port = settingsRepo.getServerPort()
    val repository = CalendarRepository(
        client = httpClient,
        baseUrlProvider = { buildBaseUrl(host, port) },
        authTokenProvider = { authToken }
    )

    val state = produceState(initialValue = emptyMap<LocalDate, List<CalendarEvent>>(), host, port, authToken) {
        value = coroutineScope {
            val remote = async { runCatching { repository.fetchCalendarEvents() }.getOrDefault(emptyList()) }.await()
            remote.groupBy { it.date }
        }
    }
    return state.value
}
