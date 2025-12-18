package be.ecam.companion.data

import be.ecam.companion.ui.screens.CalendarEvent
import be.ecam.companion.ui.screens.CalendarEventCategory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CalendarRepository(
    private val client: HttpClient,
    private val baseUrlProvider: () -> String
) {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Récupère les événements du calendrier académique depuis le serveur
     */
    suspend fun getCalendarEvents(token: String?): List<CalendarEvent> {
        return try {
            val response = client.get("${baseUrlProvider()}/api/calendar") {
                token?.let { 
                    header(HttpHeaders.Authorization, "Bearer ${it.trim().removeSurrounding("\"")}")
                }
                header(HttpHeaders.Accept, "application/json")
            }
            
            if (response.status.isSuccess()) {
                val dtos: List<CalendarEventDto> = response.body()
                dtos.mapNotNull { it.toCalendarEvent() }
            } else {
                println("Erreur calendrier: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            println("Erreur récupération calendrier: ${e.message}")
            emptyList()
        }
    }

    /**
     * Récupère l'horaire des cours depuis le serveur
     */
    suspend fun getCourseSchedule(
        token: String?,
        yearOptionId: String? = null,
        seriesId: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): List<CourseScheduleEvent> {
        return try {
            val url = buildString {
                append("${baseUrlProvider()}/api/course-schedule")
                val params = mutableListOf<String>()
                yearOptionId?.let { params.add("yearOptionId=$it") }
                seriesId?.let { params.add("seriesId=$it") }
                startDate?.let { params.add("startDate=$it") }
                endDate?.let { params.add("endDate=$it") }
                if (params.isNotEmpty()) {
                    append("?${params.joinToString("&")}")
                }
            }
            
            println("🔄 GET $url")
            
            val response = client.get(url) {
                token?.let { 
                    header(HttpHeaders.Authorization, "Bearer ${it.trim().removeSurrounding("\"")}")
                }
                header(HttpHeaders.Accept, "application/json")
            }
            
            println("📥 Response status: ${response.status}")
            
            if (response.status.isSuccess()) {
                val dtos: List<CourseScheduleDto> = response.body()
                println("📥 Reçu ${dtos.size} cours du serveur")
                dtos.mapNotNull { it.toCourseScheduleEvent(json) }
            } else {
                val body = runCatching { response.body<String>() }.getOrDefault("")
                println("❌ Erreur course-schedule: ${response.status} - $body")
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ Exception récupération course-schedule: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Récupère les year options disponibles
     */
    suspend fun getYearOptions(token: String?): List<YearOptionDto> {
        return try {
            val response = client.get("${baseUrlProvider()}/api/year-options") {
                token?.let { 
                    header(HttpHeaders.Authorization, "Bearer ${it.trim().removeSurrounding("\"")}")
                }
                header(HttpHeaders.Accept, "application/json")
            }
            if (response.status.isSuccess()) response.body() else emptyList()
        } catch (e: Exception) {
            println("Erreur year-options: ${e.message}")
            emptyList()
        }
    }

    /**
     * Récupère les series disponibles
     */
    suspend fun getSeriesNames(token: String?): List<SeriesNameDto> {
        return try {
            val response = client.get("${baseUrlProvider()}/api/series") {
                token?.let { 
                    header(HttpHeaders.Authorization, "Bearer ${it.trim().removeSurrounding("\"")}")
                }
                header(HttpHeaders.Accept, "application/json")
            }
            if (response.status.isSuccess()) response.body() else emptyList()
        } catch (e: Exception) {
            println("Erreur series: ${e.message}")
            emptyList()
        }
    }
}

// DTO pour les événements du calendrier académique
@Serializable
data class CalendarEventDto(
    val id: Int,
    val code: String,
    val title: String,
    val date: String,
    @SerialName("startTime") val startTime: String = "00:00",
    @SerialName("endTime") val endTime: String = "23:59",
    @SerialName("ownerType") val ownerType: String? = null,
    @SerialName("ownerRef") val ownerRef: String? = null,
    val description: String? = null
) {
    fun toCalendarEvent(): CalendarEvent? {
        val parsedDate = try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            return null
        }
        
        val descriptionText = buildString {
            val startClean = startTime.trim()
            val endClean = endTime.trim()
            val showTime = !(startClean == "0:00" && endClean == "23:59") &&
                !(startClean == "00:00" && endClean == "23:59")
            if (showTime) {
                append("$startTime - $endTime")
            }
            description?.let {
                if (isNotEmpty()) append("\n")
                append(it)
            }
        }
        
        val category = CalendarEventCategory.fromKey(ownerRef ?: ownerType ?: code)
        
        return CalendarEvent(
            id = "${id}_$date",
            title = title.ifBlank { code },
            description = descriptionText,
            category = category,
            date = parsedDate,
            years = emptyList()
        )
    }
}

// DTO pour l'horaire des cours
@Serializable
data class CourseScheduleDto(
    val id: Int,
    val week: Int,
    @SerialName("yearOptionId") val yearOptionId: String,
    @SerialName("groupNo") val groupNo: String,
    @SerialName("seriesJson") val seriesJson: String? = null,
    val date: String,
    @SerialName("dayName") val dayName: String,
    @SerialName("startTime") val startTime: String,
    @SerialName("endTime") val endTime: String,
    @SerialName("courseRaccourciId") val courseRaccourciId: String,
    val title: String,
    @SerialName("teachersJson") val teachersJson: String? = null,
    @SerialName("roomIds") val roomIds: String? = null,
    @SerialName("sousCourseId") val sousCourseId: String? = null
) {
    fun toCourseScheduleEvent(json: Json): CourseScheduleEvent? {
        val parsedDate = try {
            parseFlexibleDate(date)
        } catch (e: Exception) {
            println("❌ Erreur parsing date '$date': ${e.message}")
            return null
        }
        
        val seriesList = seriesJson?.let {
            try { json.decodeFromString<List<String>>(it) } catch (e: Exception) { emptyList() }
        } ?: emptyList()
        
        val teachersList = teachersJson?.let {
            try { json.decodeFromString<List<String>>(it) } catch (e: Exception) { emptyList() }
        } ?: emptyList()
        
        val roomsList = roomIds?.split(",")?.map { it.trim() } ?: emptyList()
        
        return CourseScheduleEvent(
            id = id,
            date = parsedDate,
            yearOptionId = yearOptionId,
            groupNo = groupNo,
            series = seriesList,
            startTime = startTime,
            endTime = endTime,
            courseCode = courseRaccourciId,
            courseName = title,
            teachers = teachersList,
            rooms = roomsList,
            sousCourseId = sousCourseId
        )
    }
}

/**
 * Parse une date flexible supportant ISO (YYYY-MM-DD) et US (MM/DD/YYYY)
 */
private fun parseFlexibleDate(dateStr: String): LocalDate {
    // Format ISO: 2025-11-18
    if (dateStr.contains("-") && dateStr.length == 10) {
        return LocalDate.parse(dateStr)
    }
    
    // Format US: 11/18/2025 ou format EU: 18/11/2025
    if (dateStr.contains("/")) {
        val parts = dateStr.split("/")
        if (parts.size == 3) {
            val first = parts[0].toInt()
            val second = parts[1].toInt()
            val year = parts[2].toInt()
            
            // Si le premier nombre > 12, c'est probablement DD/MM/YYYY
            return if (first > 12) {
                LocalDate(year, second, first) // DD/MM/YYYY
            } else if (second > 12) {
                LocalDate(year, first, second) // MM/DD/YYYY
            } else {
                // Ambigu - on assume MM/DD/YYYY (format US)
                LocalDate(year, first, second)
            }
        }
    }
    
    // Fallback: essayer le parsing ISO standard
    return LocalDate.parse(dateStr)
}

// Modèle pour les événements de cours
data class CourseScheduleEvent(
    val id: Int,
    val date: LocalDate,
    val yearOptionId: String,
    val groupNo: String,
    val series: List<String>,
    val startTime: String,
    val endTime: String,
    val courseCode: String,
    val courseName: String,
    val teachers: List<String>,
    val rooms: List<String>,
    val sousCourseId: String?
)

@Serializable
data class YearOptionDto(
    val id: Int,
    val yearOptionId: String,
    val formationIds: String? = null,
    val blocId: String? = null
)

@Serializable
data class SeriesNameDto(
    val id: Int,
    val seriesId: String,
    val yearId: String? = null
)
