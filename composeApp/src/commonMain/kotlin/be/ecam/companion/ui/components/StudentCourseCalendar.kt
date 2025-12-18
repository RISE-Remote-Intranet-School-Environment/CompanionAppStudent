package be.ecam.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.CalendarRepository
import be.ecam.companion.data.CourseScheduleEvent
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.data.YearOptionDto
import be.ecam.companion.data.SeriesNameDto
import be.ecam.companion.di.buildBaseUrl
import be.ecam.companion.ui.screens.CalendarScreen
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

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
    
    val calendarRepo = remember(httpClient, baseUrl) {
        CalendarRepository(httpClient) { baseUrl }
    }

    // États pour les données
    var allCourses by remember { mutableStateOf<List<CourseScheduleEvent>>(emptyList()) }
    var yearOptions by remember { mutableStateOf<List<YearOptionDto>>(emptyList()) }
    var seriesNames by remember { mutableStateOf<List<SeriesNameDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // États de sélection
    var selectedYear by remember(initialYearOption) { mutableStateOf(initialYearOption) }
    var selectedSeries by remember(initialSeries) { mutableStateOf(initialSeries) }
    
    // Données PAE de l'utilisateur
    var userYearOption by remember { mutableStateOf<String?>(null) }
    var userSeries by remember { mutableStateOf<String?>(null) }
    var userCourseCodes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var resolvedUser by remember { mutableStateOf(displayName ?: username) }

    // Charger les données initiales
    LaunchedEffect(baseUrl, bearer) {
        isLoading = true
        try {
            coroutineScope {
                val yearOptionsDeferred = async { calendarRepo.getYearOptions(bearer) }
                val seriesDeferred = async { calendarRepo.getSeriesNames(bearer) }
                val coursesDeferred = async { calendarRepo.getCourseSchedule(bearer) }
                
                yearOptions = yearOptionsDeferred.await()
                seriesNames = seriesDeferred.await()
                allCourses = coursesDeferred.await()
            }
        } catch (e: Exception) {
            println("Erreur chargement calendrier: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Charger les infos PAE de l'utilisateur
    LaunchedEffect(username, baseUrl, bearer) {
        if (username.isNullOrBlank()) return@LaunchedEffect
        
        try {
            val paeStudent = loadPaeStudentFromServer(httpClient, baseUrl, bearer, username)
            if (paeStudent != null) {
                resolvedUser = paeStudent.studentName.ifBlank { displayName ?: username }
                userYearOption = paeStudent.program
                userSeries = null // Peut être extrait du PAE si disponible
                userCourseCodes = paeStudent.courseIds
                    ?.split(';', ',', '|')
                    ?.mapNotNull { it.trim().takeIf { it.isNotEmpty() } }
                    ?.map { it.lowercase() }
                    ?.toSet()
                    ?: emptySet()
            }
        } catch (e: Exception) {
            println("Erreur chargement PAE: ${e.message}")
        }
    }

    // Sélectionner automatiquement l'année de l'utilisateur
    LaunchedEffect(userYearOption) {
        if (selectedYear == null && userYearOption != null) {
            selectedYear = userYearOption
            selectedSeries = userSeries
        }
    }

    // Sélectionner la première année disponible si rien n'est sélectionné
    LaunchedEffect(yearOptions) {
        if (selectedYear == null && yearOptions.isNotEmpty()) {
            selectedYear = yearOptions.first().yearOptionId
        }
    }

    // Filtrer les cours selon la sélection
    val filteredCourses = remember(allCourses, selectedYear, selectedSeries, userCourseCodes) {
        when {
            // Si l'utilisateur a un PAE avec des cours spécifiques
            userCourseCodes.isNotEmpty() -> {
                allCourses.filter { userCourseCodes.contains(it.courseCode.lowercase()) }
            }
            // Sinon filtrer par year option et series
            else -> {
                allCourses.filter { course ->
                    (selectedYear == null || course.yearOptionId == selectedYear) &&
                    (selectedSeries == null || course.series.contains(selectedSeries))
                }
            }
        }
    }

    // Series disponibles pour l'année sélectionnée
    val availableSeriesForYear = remember(selectedYear, allCourses, seriesNames) {
        if (selectedYear == null) {
            seriesNames.map { it.seriesId }
        } else {
            val fromCourses = allCourses
                .filter { it.yearOptionId == selectedYear }
                .flatMap { it.series }
                .distinct()
            
            if (fromCourses.isNotEmpty()) fromCourses.sorted()
            else seriesNames.map { it.seriesId }.sorted()
        }
    }

    // Auto-sélection de la première série
    LaunchedEffect(selectedYear, selectedSeries, availableSeriesForYear) {
        if (selectedYear != null && selectedSeries == null && availableSeriesForYear.isNotEmpty()) {
            selectedSeries = availableSeriesForYear.first()
        }
    }

    // Convertir les cours en format pour CalendarScreen
    val eventsByDate: Map<LocalDate, List<String>> = remember(filteredCourses) {
        filteredCourses
            .groupBy { it.date }
            .mapValues { entry ->
                entry.value.map { course ->
                    buildString {
                        append("${course.courseName} (${course.courseCode})")
                        append(" - ${course.startTime}-${course.endTime}")
                        if (course.teachers.isNotEmpty()) {
                            append(" Prof: ${course.teachers.joinToString()}")
                        }
                        if (course.rooms.isNotEmpty()) {
                            append(" Salle: ${course.rooms.joinToString()}")
                        }
                    }
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Bouton pour revenir aux cours de l'utilisateur
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
                    val label = resolvedUser?.let { "Cours de $it" } ?: "Mes cours"
                    Text(label)
                }
            }

            // Barre de filtres
            CourseFilterBar(
                yearOptions = yearOptions.map { it.yearOptionId },
                selectedYear = selectedYear,
                onYearSelected = {
                    selectedYear = it
                    selectedSeries = null
                },
                series = availableSeriesForYear,
                selectedSeries = selectedSeries,
                onSeriesSelected = { selectedSeries = it }
            )

            // Calendrier
            CalendarScreen(
                scheduledByDate = eventsByDate,
                modifier = Modifier.weight(1f),
                authToken = authToken
            )
        }
    }
}

// DTO pour le PAE étudiant
@Serializable
private data class PaeStudentDto(
    val id: Int,
    val studentId: Int,
    val studentName: String,
    val email: String,
    val program: String? = null,
    val courseIds: String? = null
)

private suspend fun loadPaeStudentFromServer(
    client: HttpClient,
    baseUrl: String,
    token: String?,
    userIdentifier: String?
): PaeStudentDto? {
    if (userIdentifier.isNullOrBlank()) return null
    
    return try {
        val response = client.get("$baseUrl/api/pae-students/by-name/$userIdentifier") {
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        if (response.status.value in 200..299) {
            response.body<PaeStudentDto>()
        } else {
            null
        }
    } catch (e: Exception) {
        println("Erreur loadPaeStudent: ${e.message}")
        null
    }
}
