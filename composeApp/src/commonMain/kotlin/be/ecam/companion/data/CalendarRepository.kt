package be.ecam.companion.data

import be.ecam.companion.ui.components.CourseEvent
import be.ecam.companion.ui.components.toCalendarEvent
import be.ecam.companion.ui.screens.CalendarEvent
import be.ecam.companion.ui.screens.CalendarEventCategory
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class CalendarRepository(
    private val client: HttpClient,
    private val baseUrlProvider: () -> String,
    private val authTokenProvider: () -> String? = { null }
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchCalendarEvents(): List<CalendarEvent> =
        apiGet<List<CalendarEventDto>>("/api/calendar")
            .mapNotNull { dto -> dto.toCalendarEventOrNull() }

    suspend fun fetchCourseEvents(): List<CourseEvent> {
        val schedule = apiGet<List<CourseScheduleDto>>("/api/course-schedule")
        return schedule.mapNotNull { it.toCourseEventOrNull(json) }
    }

    private suspend inline fun <reified T> apiGet(path: String): T {
        val token = authTokenProvider()
            ?.trim()
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() }

        return client.get("${baseUrlProvider()}$path") {
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }.body()
    }
}

private fun parseDateFlexible(raw: String): LocalDate? {
    // Accept ISO yyyy-MM-dd or US-style M/d/yyyy
    runCatching { LocalDate.parse(raw) }.getOrNull()?.let { return it }
    val clean = raw.replace("\\", "/")
    val parts = clean.split("/", "-").mapNotNull { it.toIntOrNull() }
    if (parts.size == 3) {
        val (y, m, d) = if (parts[0] > 31) {
            // Probably yyyy,mm,dd
            Triple(parts[0], parts[1], parts[2])
        } else {
            // Assume M/d/yyyy
            Triple(parts[2], parts[0], parts[1])
        }
        return runCatching { LocalDate(y, m, d) }.getOrNull()
    }
    return null
}

@Serializable
private data class CalendarEventDto(
    val id: Int,
    val code: String,
    val title: String,
    val date: String,
    @SerialName("startTime") val startTime: String,
    @SerialName("endTime") val endTime: String,
    val groupCode: String? = null,
    val ownerType: String? = null,
    val ownerRef: String? = null
) {
    fun toCalendarEventOrNull(): CalendarEvent? {
        val parsedDate = parseDateFlexible(date) ?: return null
        val description = buildString {
            append("$startTime - $endTime")
            ownerRef?.takeIf { it.isNotBlank() }?.let { append("\nRef: $it") }
            groupCode?.takeIf { it.isNotBlank() }?.let { append("\nGroupe: $it") }
        }
        val category = CalendarEventCategory.fromKey(ownerRef ?: ownerType ?: code)
        return CalendarEvent(
            id = "${id}_$date",
            title = title.ifBlank { code },
            description = description,
            category = category,
            date = parsedDate,
            years = emptyList()
        )
    }
}

@Serializable
private data class CourseScheduleDto(
    val id: Int,
    val week: Int,
    @SerialName("yearOptionId") val yearOptionId: String,
    @SerialName("groupNo") val groupNo: String,
    @SerialName("seriesJson") val seriesJson: String? = null,
    val date: String,
    val dayName: String,
    @SerialName("startTime") val startTime: String,
    @SerialName("endTime") val endTime: String,
    @SerialName("courseRaccourciId") val courseRaccourciId: String,
    val title: String,
    @SerialName("teachersJson") val teachersJson: String? = null,
    @SerialName("roomIds") val roomIds: String? = null,
    @SerialName("sousCourseId") val sousCourseId: String? = null
) {
    fun toCourseEventOrNull(json: Json): CourseEvent? {
        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        val series = parseListField(seriesJson, json)
        val teachers = parseListField(teachersJson, json)
        val rooms = roomIds
            ?.split(';', ',', '/')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        val code = courseRaccourciId.ifBlank { sousCourseId ?: title.take(8) }
        return CourseEvent(
            date = parsedDate,
            yearOption = yearOptionId,
            series = series.ifEmpty { listOf(groupNo) },
            courseCode = code,
            courseName = title,
            startTime = startTime,
            endTime = endTime,
            teachers = teachers,
            rooms = rooms
        )
    }
}

private fun parseListField(raw: String?, json: Json): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        val arr = json.parseToJsonElement(raw).jsonArray
        arr.mapNotNull { elem ->
            val content = elem.jsonPrimitive.content
            content.takeIf { it.isNotBlank() }
        }
    } catch (_: Throwable) {
        raw.split(';', ',', '/').map { it.trim() }.filter { it.isNotBlank() }
    }
}
