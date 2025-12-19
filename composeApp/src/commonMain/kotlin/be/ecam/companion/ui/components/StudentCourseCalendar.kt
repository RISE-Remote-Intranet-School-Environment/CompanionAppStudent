package be.ecam.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    // Codes de cours de l'utilisateur (courseRaccourciId)
    var userCourseCodes by remember { mutableStateOf<Set<String>>(emptySet()) }
    var resolvedUser by remember { mutableStateOf(displayName ?: username) }
    var showOnlyUserCourses by remember { mutableStateOf(true) }

    // Charger les données initiales
    LaunchedEffect(baseUrl, bearer) {
        isLoading = true
        try {
            println("📅 Chargement calendrier depuis: $baseUrl/api/course-schedule")
            coroutineScope {
                val yearOptionsDeferred = async { calendarRepo.getYearOptions(bearer) }
                val seriesDeferred = async { calendarRepo.getSeriesNames(bearer) }
                val coursesDeferred = async { calendarRepo.getCourseSchedule(bearer) }
                
                yearOptions = yearOptionsDeferred.await()
                seriesNames = seriesDeferred.await()
                allCourses = coursesDeferred.await()
                
                println("📅 Cours chargés: ${allCourses.size}")
                println("📅 YearOptions: ${yearOptions.map { it.yearOptionId }}")
                println("📅 Exemple cours: ${allCourses.take(3).map { "${it.courseCode} -> ${it.sousCourseId}" }}")
            }
        } catch (e: Exception) {
            println("❌ Erreur chargement calendrier: ${e.message}")
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
                userYearOption = paeStudent.yearOptionId
                userSeries = paeStudent.seriesId
                
                // Extraire les codes de cours depuis les courseIds du PAE
                // Les courseIds dans pae_students sont les codes courts (ex: "AS5T", "BC3C")
                val courseIds = paeStudent.courseIds
                    ?.split(';', ',', '|')
                    ?.mapNotNull { it.trim().uppercase().takeIf { s -> s.isNotEmpty() } }
                    ?.toSet()
                    ?: emptySet()
                
                userCourseCodes = courseIds
                println("📚 PAE user: yearOption=${paeStudent.yearOptionId}, series=${paeStudent.seriesId}, courseCodes=$courseIds")
            }
        } catch (e: Exception) {
            println("❌ Erreur chargement PAE: ${e.message}")
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
    val filteredCourses = remember(allCourses, selectedYear, selectedSeries, userCourseCodes, userYearOption, showOnlyUserCourses) {
        when {
            // Mode "Mes cours" : filtrer par codes de cours du PAE ET year option de l'utilisateur
            showOnlyUserCourses && userCourseCodes.isNotEmpty() -> {
                val filtered = allCourses.filter { course ->
                    // Le courseRaccourciId doit correspondre à un des codes du PAE
                    val codeMatch = userCourseCodes.contains(course.courseCode.uppercase())
                    // Et optionnellement filtrer par yearOption si disponible
                    val yearMatch = userYearOption == null || course.yearOptionId == userYearOption
                    codeMatch && yearMatch
                }
                println("📅 Filtré par PAE: ${filtered.size} cours (codes: $userCourseCodes, yearOption: $userYearOption)")
                filtered
            }
            // Mode exploration : filtrer par year option et series
            else -> {
                allCourses.filter { course ->
                    val yearMatch = selectedYear == null || course.yearOptionId == selectedYear
                    val seriesMatch = selectedSeries == null || 
                        course.series.isEmpty() || 
                        course.series.any { it.equals(selectedSeries, ignoreCase = true) }
                    yearMatch && seriesMatch
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Boutons de mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bouton "Mes cours" (actif si l'utilisateur a un PAE)
                FilterChip(
                    selected = showOnlyUserCourses && userCourseCodes.isNotEmpty(),
                    onClick = { showOnlyUserCourses = true },
                    enabled = userCourseCodes.isNotEmpty(),
                    label = { 
                        val label = resolvedUser?.let { "Cours de $it" } ?: "Mes cours"
                        Text(label) 
                    }
                )
                
                // Bouton "Explorer"
                FilterChip(
                    selected = !showOnlyUserCourses || userCourseCodes.isEmpty(),
                    onClick = { showOnlyUserCourses = false },
                    label = { Text("Explorer") }
                )
            }

            // Barre de filtres (visible uniquement en mode exploration)
            if (!showOnlyUserCourses || userCourseCodes.isEmpty()) {
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
            }

            // Info sur le filtrage actuel
            if (showOnlyUserCourses && userCourseCodes.isNotEmpty()) {
                Text(
                    text = "Affichage de ${filteredCourses.size} séances pour ${userCourseCodes.size} cours de votre PAE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

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
    val studentId: Int? = null,
    val studentName: String = "",
    val email: String = "",
    val yearOptionId: String? = null,
    val seriesId: String? = null,
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
        // Essayer par email d'abord, puis par nom
        val endpoints = listOf(
            "$baseUrl/api/pae-students/by-email/$userIdentifier",
            "$baseUrl/api/pae-students/by-name/$userIdentifier"
        )
        
        for (endpoint in endpoints) {
            try {
                println("🔍 Essai chargement PAE: $endpoint")
                val response = client.get(endpoint) {
                    token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                }
                if (response.status.value in 200..299) {
                    val pae = response.body<PaeStudentDto>()
                    println("✅ PAE trouvé: yearOption=${pae.yearOptionId}, courses=${pae.courseIds}")
                    return pae
                }
            } catch (e: Exception) {
                println("⚠️ Endpoint $endpoint: ${e.message}")
            }
        }
        null
    } catch (e: Exception) {
        println("❌ Erreur loadPaeStudent: ${e.message}")
        null
    }
}
