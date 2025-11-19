package be.ecam.companion.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import companion.composeapp.generated.resources.Res
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Serializable
private data class RawCalendarEvent(
    val id: String,
    val type: String? = null,
    val start: String? = null,
    val end: String? = null,
    val date: String? = null,
    val description: String = "",
    @SerialName("annees_concernees") val years: List<String> = emptyList(),
    @SerialName("categorie") val category: String? = null,
    val title: String = ""
)

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
                else -> Unknown
            }
    }
}

private object CalendarEventsLoader {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): Map<LocalDate, List<CalendarEvent>> {
        val bytes = Res.readBytes("files/ecam_calendar_events_2025_2026.json")
        val raw = json.decodeFromString<List<RawCalendarEvent>>(bytes.decodeToString())
        val events = raw.flatMap { event ->
            val dates = expandDates(event)
            val category = CalendarEventCategory.fromKey(event.category)
            dates.map { day ->
                CalendarEvent(
                    id = "${event.id}_$day",
                    title = event.title,
                    description = event.description,
                    category = category,
                    date = day,
                    years = event.years
                )
            }
        }
        return events.groupBy { it.date }
    }

    private fun expandDates(event: RawCalendarEvent): List<LocalDate> {
        event.date?.let { return listOf(parseDate(it)) }
        val start = event.start?.let(::parseDate) ?: return emptyList()
        val end = event.end?.let(::parseDate) ?: start
        val result = mutableListOf<LocalDate>()
        var cursor = start
        while (cursor <= end) {
            result += cursor
            cursor = cursor.plus(1, DateTimeUnit.DAY)
        }
        return result
    }

    private fun parseDate(value: String): LocalDate {
        val parts = value.split("-")
        return LocalDate(
            parts[0].toInt(),
            parts[1].toInt(),
            parts[2].toInt()
        )
    }
}

@Composable
fun rememberCalendarEventsByDate(): Map<LocalDate, List<CalendarEvent>> {
    val state = produceState(initialValue = emptyMap<LocalDate, List<CalendarEvent>>()) {
        value = CalendarEventsLoader.load()
    }
    return state.value
}
