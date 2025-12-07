package be.ecam.companion.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    var filtersExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { filtersExpanded = !filtersExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (filtersExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (filtersExpanded) "Collapse filters" else "Expand filters"
            )
            Spacer(Modifier.width(8.dp))
            Text("Filtres")
        }

        if (filtersExpanded) {
            Text("Year Option")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                yearOptions.forEach { year ->
                    FilterChip(
                        selected = selectedYear == year,
                        onClick = { onYearSelected(year) },
                        label = { Text(year) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Series")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
}
