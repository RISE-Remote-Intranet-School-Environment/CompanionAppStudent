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
        return try {
            val bytes = Res.readBytes("files/ecam_calendar_events_2025_2026.json")
            val content = bytes.decodeToString()
            val rawEvents: List<RawCalendarEvent> = json.decodeFromString(content)
            rawEvents.flatMap { raw -> convertRawEvent(raw) }
        } catch (e: Exception) {
            println("Erreur chargement calendrier local: ${e.message}")
            emptyList()
        }
    }

    private fun convertRawEvent(raw: RawCalendarEvent): List<CalendarEvent> {
        val category = CalendarEventCategory.fromKey(raw.category ?: raw.type ?: "autre")
        
        // Si c'est un événement sur plusieurs jours
        if (!raw.start.isNullOrBlank() && !raw.end.isNullOrBlank()) {
            return try {
                val startDate = LocalDate.parse(raw.start)
                val endDate = LocalDate.parse(raw.end)
                generateDateRange(startDate, endDate).map { date ->
                    CalendarEvent(
                        id = "${raw.id}_$date",
                        title = raw.title,
                        description = raw.description,
                        category = category,
                        date = date,
                        years = raw.years
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
        
        // Événement sur un seul jour
        val dateStr = raw.date ?: raw.start
        if (dateStr.isNullOrBlank()) return emptyList()
        
        return try {
            val date = LocalDate.parse(dateStr)
            listOf(
                CalendarEvent(
                    id = raw.id,
                    title = raw.title,
                    description = raw.description,
                    category = category,
                    date = date,
                    years = raw.years
                )
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun generateDateRange(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = start
        while (current <= end) {
            dates.add(current)
            current = current.plus(1, DateTimeUnit.DAY)
        }
        return dates
    }
}
