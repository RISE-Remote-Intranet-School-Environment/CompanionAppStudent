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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDate
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
    var myCourses by remember { mutableStateOf<List<CourseScheduleEvent>>(emptyList()) }
    var yearOptions by remember { mutableStateOf<List<YearOptionDto>>(emptyList()) }
    var seriesNames by remember { mutableStateOf<List<SeriesNameDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // États de sélection
    var selectedYear by remember(initialYearOption) { mutableStateOf(initialYearOption) }
    var selectedSeries by remember(initialSeries) { mutableStateOf(initialSeries) }
    
    var resolvedUser by remember { mutableStateOf(displayName ?: username) }
    var showOnlyUserCourses by remember { mutableStateOf(true) }

    // Charger les données initiales (Calendrier global + Personnel)
    LaunchedEffect(baseUrl, bearer) {
        isLoading = true
        try {
            coroutineScope {
                val yearOptionsDeferred = async { calendarRepo.getYearOptions(bearer) }
                val seriesDeferred = async { calendarRepo.getSeriesNames(bearer) }
                val coursesDeferred = async { calendarRepo.getCourseSchedule(bearer) }
                val myCoursesDeferred = async { calendarRepo.getMySchedule(bearer) }
                
                yearOptions = yearOptionsDeferred.await()
                seriesNames = seriesDeferred.await()
                allCourses = coursesDeferred.await()
                myCourses = myCoursesDeferred.await()
                
                println("📚 Tous les cours: ${allCourses.size}, Mes cours: ${myCourses.size}")
            }
        } catch (e: Exception) {
            println("❌ Erreur chargement calendrier: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Sélectionner la première année disponible si rien n'est sélectionné
    LaunchedEffect(yearOptions) {
        if (selectedYear == null && yearOptions.isNotEmpty()) {
            selectedYear = yearOptions.first().yearOptionId
        }
    }

    // Logique de filtrage simplifiée
    val filteredCourses = remember(allCourses, myCourses, selectedYear, selectedSeries, showOnlyUserCourses) {
        if (showOnlyUserCourses) {
            // MODE MES COURS : Utiliser directement les cours filtrés par le backend
            myCourses
        } else {
            // MODE EXPLORER : On filtre par année et série sélectionnées
            allCourses.filter { course ->
                val yearMatch = selectedYear == null || course.yearOptionId == selectedYear
                val seriesMatch = selectedSeries == null || 
                    course.series.isEmpty() || 
                    course.series.any { it.equals(selectedSeries, ignoreCase = true) }
                yearMatch && seriesMatch
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
                // Bouton "Mes cours"
                FilterChip(
                    selected = showOnlyUserCourses,
                    onClick = { showOnlyUserCourses = true },
                    label = { 
                        val label = resolvedUser?.let { "Cours de $it" } ?: "Mes cours"
                        Text(label) 
                    }
                )
                
                // Bouton "Explorer"
                FilterChip(
                    selected = !showOnlyUserCourses,
                    onClick = { showOnlyUserCourses = false },
                    label = { Text("Explorer") }
                )
            }

            // Barre de filtres (visible uniquement en mode exploration)
            if (!showOnlyUserCourses) {
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
            if (showOnlyUserCourses) {
                if (myCourses.isEmpty()) {
                    Text(
                        text = "Aucun cours trouvé dans votre PAE.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    Text(
                        text = "Affichage de ${myCourses.size} séances personnelles",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
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