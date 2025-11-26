package be.ecam.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CourseFilterBar(
    yearOptions: List<String>,
    selectedYear: String?,
    onYearSelected: (String) -> Unit,
    series: List<String>,
    selectedSeries: String?,
    onSeriesSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Filtres", modifier = Modifier.padding(vertical = 8.dp))

        Text("Year Option")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            yearOptions.forEach { year ->
                FilterChip(
                    selected = selectedYear == year,
                    onClick = { onYearSelected(year) },
                    label = { Text(year) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text("Series")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            series.forEach { ser ->
                FilterChip(
                    selected = selectedSeries == ser,
                    onClick = { onSeriesSelected(ser) },
                    label = { Text(ser) }
                )
            }
        }
    }
}
