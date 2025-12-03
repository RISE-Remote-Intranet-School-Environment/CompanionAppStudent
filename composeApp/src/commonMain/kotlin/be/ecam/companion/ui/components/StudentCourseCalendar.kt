package be.ecam.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import be.ecam.companion.ui.screens.CalendarScreen

@Composable
fun StudentCourseCalendar(
    modifier: Modifier = Modifier,
    initialYearOption: String? = null,
    initialSeries: String? = null
) {
    Column(modifier = modifier.fillMaxSize()) {
        val allCourses = rememberCourseEvents()

        var selectedYear by remember(initialYearOption) { mutableStateOf(initialYearOption) }
        var selectedSeries by remember(initialSeries) { mutableStateOf(initialSeries) }

        val availableYears = allCourses.map { it.yearOption }.distinct()
        val availableSeries = allCourses.flatMap { it.series }.distinct()

        val filtered = allCourses.filter { c ->
            (selectedYear == null || c.yearOption == selectedYear) &&
                (selectedSeries == null || c.series.contains(selectedSeries))
        }

        // Convertir en Map<LocalDate, List<String>> pour CalendarScreen
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
                    allCourses
                        .filter { it.yearOption == selectedYear }
                        .flatMap { it.series }
                        .distinct()
                        .sorted()
                }

            LaunchedEffect(selectedYear, selectedSeries, seriesForSelectedYear) {
                if (selectedYear != null && selectedSeries == null && seriesForSelectedYear.isNotEmpty()) {
                    selectedSeries = seriesForSelectedYear.first()
                }
            }


            CourseFilterBar(
                yearOptions = availableYears,
                selectedYear = selectedYear,
                onYearSelected = {
                    selectedYear = it
                    selectedSeries = null    // OBLIGATOIRE sinon le filtre ne s’actualise pas !
                },
                series = seriesForSelectedYear,
                selectedSeries = selectedSeries,
                onSeriesSelected = { selectedSeries = it }
            )

            CalendarScreen(
                scheduledByDate = eventsByDateStrings,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
