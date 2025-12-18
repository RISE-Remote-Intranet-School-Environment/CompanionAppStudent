package be.ecam.companion.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.buildBaseUrl
import be.ecam.companion.ui.screens.CalendarScreen
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import org.koin.compose.koinInject
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Composable
fun StudentCourseCalendar(
    modifier: Modifier = Modifier,
    initialYearOption: String? = null,
    initialSeries: String? = null,
    username: String? = null,
    displayName: String? = null,
    authToken: String? = null
) {
    val httpClient = koinInject<HttpClient>()
    val settingsRepo = koinInject<SettingsRepository>()
    val host by settingsRepo.serverHostFlow.collectAsState(settingsRepo.getServerHost())
    val port by settingsRepo.serverPortFlow.collectAsState(settingsRepo.getServerPort())
    val baseUrl = buildBaseUrl(host, port)
    val bearer = authToken?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }
    val json = remember { Json { ignoreUnknownKeys = true } }

    val serverYearOptions by produceState(initialValue = emptyList<String>(), baseUrl, bearer) {
        value = runCatching {
            val payload = httpClient.get("$baseUrl/api/year-options") {
                bearer?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                header(HttpHeaders.Accept, "application/json")
            }.body<String>()
            json.decodeFromString<List<YearOptionDto>>(payload).map { it.yearOptionId }
        }.getOrDefault(emptyList())
    }

    val serverSeries by produceState(initialValue = emptyList<String>(), baseUrl, bearer) {
        value = runCatching {
            val payload = httpClient.get("$baseUrl/api/series") {
                bearer?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                header(HttpHeaders.Accept, "application/json")
            }.body<String>()
            json.decodeFromString<List<SeriesDto>>(payload).map { it.seriesId }
        }.getOrDefault(emptyList())
    }

    Column(modifier = modifier.fillMaxSize()) {
        val allCourses = rememberCourseEvents(authToken)

        var selectedYear by remember(initialYearOption) { mutableStateOf(initialYearOption) }
        var selectedSeries by remember(initialSeries) { mutableStateOf(initialSeries) }
        var userYearOption by remember { mutableStateOf<String?>(null) }
        var userSeries by remember { mutableStateOf<String?>(null) }
        var resolvedUser by remember { mutableStateOf(displayName ?: username) }
        var userCourseCodes by remember { mutableStateOf<Set<String>>(emptySet()) }

        val paeStudent by produceState<PaeStudentDto?>(initialValue = null, username, baseUrl, bearer) {
            value = runCatching {
                loadPaeStudentFromServer(
                    client = httpClient,
                    baseUrl = baseUrl,
                    token = bearer,
                    userIdentifier = username
                )
            }.getOrNull()
        }

        LaunchedEffect(username) {
            val target = username?.takeIf { it.isNotBlank() } ?: "moi"
            resolvedUser = paeStudent?.studentName ?: displayName ?: target
            userYearOption = paeStudent?.program
            userSeries = null
            userCourseCodes = paeStudent?.courseIds
                ?.split(';', ',', '|')
                ?.mapNotNull { it.trim().takeIf { it.isNotEmpty() } }
                ?.map { it.lowercase() }
                ?.toSet()
                ?: emptySet()
        }

        LaunchedEffect(userYearOption) {
            if (selectedYear == null && userYearOption != null) {
                selectedYear = userYearOption
                selectedSeries = userSeries
            }
        }

        val coursesForUser = when {
            userCourseCodes.isNotEmpty() ->
                allCourses.filter { userCourseCodes.contains(it.courseCode.lowercase()) }
            userYearOption != null ->
                allCourses.filter { it.yearOption == userYearOption }
            else -> allCourses
        }

        val availableYears = serverYearOptions.takeIf { it.isNotEmpty() }
            ?: coursesForUser.map { it.yearOption }.distinct()
        val availableSeries = serverSeries.takeIf { it.isNotEmpty() }
            ?: coursesForUser.flatMap { it.series }.distinct()

        LaunchedEffect(availableYears) {
            if (selectedYear == null && availableYears.isNotEmpty()) {
                selectedYear = availableYears.first()
                selectedSeries = null
            }
        }

        val filtered = coursesForUser.filter { c ->
            (selectedYear == null || c.yearOption == selectedYear) &&
                (selectedSeries == null || c.series.contains(selectedSeries))
        }

        // Convert to Map<LocalDate, List<String>> for CalendarScreen
        val eventsByDateStrings: Map<kotlinx.datetime.LocalDate, List<String>> =
            filtered.groupBy { it.date }
                .mapValues { entry ->
                    entry.value.map { course ->
                        "${course.courseName} (${course.courseCode}) - ${course.startTime}-${course.endTime} Prof: ${course.teachers.joinToString()} Salle: ${course.rooms.joinToString()}"
                    }
                }

        Column(modifier = Modifier.fillMaxSize()) {

            val seriesForSelectedYear =
                if (selectedYear == null) {
                    emptyList()
                } else {
                    val baseSeries = serverSeries.takeIf { it.isNotEmpty() }
                        ?: coursesForUser
                            .filter { it.yearOption == selectedYear }
                            .flatMap { it.series }
                    baseSeries.distinct().sorted()
                }

            LaunchedEffect(selectedYear, selectedSeries, seriesForSelectedYear) {
                if (selectedYear != null && selectedSeries == null && seriesForSelectedYear.isNotEmpty()) {
                    selectedSeries = seriesForSelectedYear.first()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                Button(
                    enabled = userYearOption != null,
                    onClick = {
                        selectedYear = userYearOption
                        selectedSeries = userSeries
                    }
                ) {
                    val label = resolvedUser?.let { "Cours de $it" } ?: "Cours de Nirina Crépin"
                    Text(label)
                }
            }

            CourseFilterBar(
                yearOptions = availableYears,
                selectedYear = selectedYear,
                onYearSelected = {
                    selectedYear = it
                    selectedSeries = null    // mandatory to refresh series list
                },
                series = seriesForSelectedYear,
                selectedSeries = selectedSeries,
                onSeriesSelected = { selectedSeries = it }
            )

            CalendarScreen(
                scheduledByDate = eventsByDateStrings,
                modifier = Modifier.weight(1f),
                authToken = authToken
            )
        }
    }
}

private data class PaeStudentDto(
    val studentId: Int,
    val studentName: String,
    val email: String,
    val program: String? = null,
    val courseIds: String? = null
)

@Serializable
private data class YearOptionDto(val id: Int, val yearOptionId: String, val formationIds: String? = null, val blocId: String? = null)

@Serializable
private data class SeriesDto(val id: Int, val seriesId: String, val yearId: String? = null)

private suspend fun loadPaeStudentFromServer(
    client: HttpClient,
    baseUrl: String,
    token: String?,
    userIdentifier: String?
): PaeStudentDto? {
    val bearer = token?.takeIf { it.isNotBlank() }
    val students: List<PaeStudentDto> = client.get("$baseUrl/api/pae-students") {
        bearer?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        header(HttpHeaders.Accept, "application/json")
    }.body()

    if (students.isEmpty()) return null
    val target = userIdentifier?.trim()?.lowercase()
    return students.firstOrNull { dto ->
        val emailLower = dto.email.lowercase()
        val username = emailLower.substringBefore("@")
        target != null && (emailLower == target || username == target)
    } ?: students.firstOrNull()
}
