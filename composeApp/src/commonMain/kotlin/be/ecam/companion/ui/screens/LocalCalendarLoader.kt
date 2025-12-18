package be.ecam.companion.ui.screens

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

internal object LocalCalendarLoader {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): List<CalendarEvent> {
        val bytes = Res.readBytes("files/ecam_calendar_events_2025_2026.json")
        val raw = json.decodeFromString<List<RawCalendarEvent>>(bytes.decodeToString())
        return raw.flatMap { event ->
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
        val parts = value.split("-","/") // fallback for different formats
        return LocalDate(
            parts[0].toInt(),
            parts[1].toInt(),
            parts[2].toInt()
        )
    }
}
