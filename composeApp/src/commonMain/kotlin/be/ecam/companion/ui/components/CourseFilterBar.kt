package be.ecam.companion.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseFilterBar(
    yearOptions: List<String>,
    selectedYear: String?,
    onYearSelected: (String?) -> Unit,
    series: List<String>,
    selectedSeries: String?,
    onSeriesSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Sélecteur d'année
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Année:",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(end = 4.dp)
            )

            yearOptions.forEach { year ->
                FilterChip(
                    selected = selectedYear == year,
                    onClick = { onYearSelected(year) },
                    label = { Text(year) }
                )
            }
        }

        // Sélecteur de série
        if (series.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Série:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 4.dp)
                )

                // Option "Toutes"
                FilterChip(
                    selected = selectedSeries == null,
                    onClick = { onSeriesSelected(null) },
                    label = { Text("Toutes") }
                )

                series.forEach { s ->
                    FilterChip(
                        selected = selectedSeries == s,
                        onClick = { onSeriesSelected(s) },
                        label = { Text(s) }
                    )
                }
            }
        }
    }
}
