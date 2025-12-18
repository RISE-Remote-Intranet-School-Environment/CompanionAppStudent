package be.ecam.companion.ui.screens

import androidx.compose.runtime.*
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
    Course("course", Color(0xFF1976D2)),
    Remote("remote", Color(0xFFE91E63)),
    Autre("autre", Color(0xFF6D6D6D));

    companion object {
        fun fromKey(key: String): CalendarEventCategory {
            val lowerKey = key.lowercase()
            return entries.find { lowerKey.contains(it.key) } ?: Autre
        }
    }
}

/**
 * Composable qui charge les événements du calendrier (distant + local) et les retourne groupés par date.
 */
@Composable
fun rememberCalendarEventsByDate(authToken: String? = null): Map<LocalDate, List<CalendarEvent>> {
    val httpClient = koinInject<HttpClient>()
    val settingsRepo = koinInject<SettingsRepository>()
    val host = settingsRepo.getServerHost()
    val port = settingsRepo.getServerPort()
    
    val repository = remember(httpClient, host, port) {
        CalendarRepository(
            client = httpClient,
            baseUrlProvider = { buildBaseUrl(host, port) }
        )
    }

    val state = produceState(initialValue = emptyMap<LocalDate, List<CalendarEvent>>(), host, port, authToken) {
        value = coroutineScope {
            // Charger depuis le serveur
            val remoteEvents = async { 
                runCatching { repository.getCalendarEvents(authToken) }.getOrDefault(emptyList()) 
            }
            // Charger depuis le fichier local
            val localEvents = async { 
                runCatching { LocalCalendarLoader.load() }.getOrDefault(emptyList()) 
            }
            
            // Combiner les deux sources
            val allEvents = remoteEvents.await() + localEvents.await()
            
            // Grouper par date
            allEvents.groupBy { it.date }
        }
    }

    return state.value
}
