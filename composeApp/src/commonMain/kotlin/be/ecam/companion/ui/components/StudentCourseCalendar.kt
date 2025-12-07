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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.PaeRepository
import be.ecam.companion.ui.screens.CalendarScreen

@Composable
fun StudentCourseCalendar(
    modifier: Modifier = Modifier,
    initialYearOption: String? = null,
    initialSeries: String? = null,
    username: String? = null
) {
    Column(modifier = modifier.fillMaxSize()) {
        val allCourses = rememberCourseEvents()

        var selectedYear by remember(initialYearOption) { mutableStateOf(initialYearOption) }
        var selectedSeries by remember(initialSeries) { mutableStateOf(initialSeries) }
        var userYearOption by remember { mutableStateOf<String?>(null) }
        var userSeries by remember { mutableStateOf<String?>(null) }
        var resolvedUser by remember { mutableStateOf(username) }

        LaunchedEffect(username) {
            val fallback = "ncrepin"
            val target = username?.takeIf { it.isNotBlank() } ?: fallback
            val db = PaeRepository.load()
            var student = db.students.firstOrNull { it.username == target || it.email == target }
            if (student == null && target != fallback) {
                student = db.students.firstOrNull { it.username == fallback || it.email == fallback }
            }
            resolvedUser = student?.studentName ?: target
            val program = student?.records?.firstOrNull()?.program
            userYearOption = program
            userSeries = null
        }

        LaunchedEffect(userYearOption) {
            if (selectedYear == null && userYearOption != null) {
                selectedYear = userYearOption
                selectedSeries = userSeries
            }
        }

        val availableYears = allCourses.map { it.yearOption }.distinct()
        val availableSeries = allCourses.flatMap { it.series }.distinct()

        val filtered = allCourses.filter { c ->
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
                modifier = Modifier.weight(1f)
            )
        }
    }
}
