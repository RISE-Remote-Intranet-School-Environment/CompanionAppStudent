package be.ecam.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.FormationBlock
import be.ecam.companion.data.FormationCourse
import be.ecam.companion.ui.CourseRef
import kotlin.math.roundToInt

@Composable
fun CoursesFormationBlocScreen(
    program: ProgramCardData,
    block: FormationBlock,
    programs: List<ProgramCardData>,
    databaseYear: String?,
    onFormationSelected: (ProgramCardData) -> Unit,
    onBlockSelected: (FormationBlock) -> Unit,
    onCourseSelected: (CourseRef) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWide = maxWidth > 900.dp
        val sidebarWidth = 550.dp
        val sidebarMaxHeight = maxHeight
        val availablePeriods = remember(block) {
            block.courses.flatMap { it.periods }
                .distinct()
                .filter { it.isNotBlank() }
                .ifEmpty { listOf("Q1", "Q2", "Q1 - Q2") }
        }
        var periodFilter by remember(block) { mutableStateOf("Tous") }
        var sortOption by remember(block) { mutableStateOf(SortOption.Default) }
        val filteredCourses = remember(block, periodFilter, sortOption) {
            applySortAndFilter(block.courses, periodFilter, sortOption)
        }
        val sortedBlocks = remember(program) { program.formation.blocks.sortedByBlocOrder() }

        if (isWide) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1580.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = sidebarWidth)
                            .heightIn(max = sidebarMaxHeight),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FormationHeroCard(
                            program = program,
                            year = databaseYear,
                            totalCourses = filteredCourses.size,
                            block = block,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FilterPanel(
                            availablePeriods = availablePeriods,
                            selectedPeriod = periodFilter,
                            onPeriodSelected = { periodFilter = it },
                            sortOption = sortOption,
                            onSortSelected = { sortOption = it },
                            totalCourses = filteredCourses.size,
                            selectedFormation = program,
                            formations = programs,
                            onFormationSelected = onFormationSelected,
                            blocks = sortedBlocks,
                            selectedBlock = block,
                            onBlockSelected = onBlockSelected,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    BlockDetails(
                        block = block,
                        courses = filteredCourses,
                        onCourseSelected = onCourseSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FormationHeroCard(
                    program = program,
                    year = databaseYear,
                    totalCourses = filteredCourses.size,
                    block = block,
                    modifier = Modifier.fillMaxWidth()
                )
                FilterPanel(
                    availablePeriods = availablePeriods,
                    selectedPeriod = periodFilter,
                    onPeriodSelected = { periodFilter = it },
                    sortOption = sortOption,
                    onSortSelected = { sortOption = it },
                    totalCourses = filteredCourses.size,
                    selectedFormation = program,
                    formations = programs,
                    onFormationSelected = onFormationSelected,
                    blocks = sortedBlocks,
                    selectedBlock = block,
                    onBlockSelected = onBlockSelected,
                    modifier = Modifier.fillMaxWidth()
                )
                BlockDetails(
                    block = block,
                    courses = filteredCourses,
                    onCourseSelected = onCourseSelected
                )
            }
        }
    }
}

@Composable
private fun FilterPanel(
    availablePeriods: List<String>,
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    sortOption: SortOption,
    onSortSelected: (SortOption) -> Unit,
    totalCourses: Int,
    selectedFormation: ProgramCardData,
    formations: List<ProgramCardData>,
    onFormationSelected: (ProgramCardData) -> Unit,
    blocks: List<FormationBlock>,
    selectedBlock: FormationBlock,
    onBlockSelected: (FormationBlock) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Filtres et tri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Cours trouves : $totalCourses",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Formation", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                formations.forEach { program ->
                    AssistChip(
                        onClick = { onFormationSelected(program) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FormationAvatar(program.formation.id, size = 18.dp)
                                Text(program.title)
                            }
                        },
                        colors = if (program == selectedFormation) {
                            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            AssistChipDefaults.assistChipColors()
                        }
                    )
                }
            }
            if (blocks.isNotEmpty()) {
                Text("Bloc", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    blocks.forEach { block ->
                        AssistChip(
                            onClick = { onBlockSelected(block) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BlocAvatar(block.name, size = 16.dp)
                                    Text(block.name)
                                }
                            },
                            colors = if (block == selectedBlock) {
                                AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                AssistChipDefaults.assistChipColors()
                            }
                        )
                    }
                }
            }
            Text("Periodes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val periods = listOf("Tous") + availablePeriods
                periods.forEach { period ->
                    AssistChip(
                        onClick = { onPeriodSelected(period) },
                        label = { Text(period) },
                        colors = if (period == selectedPeriod) {
                            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            AssistChipDefaults.assistChipColors()
                        }
                    )
                }
            }
            Text("Tri", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOption.values().forEach { option ->
                    AssistChip(
                        onClick = { onSortSelected(option) },
                        label = { Text(option.label()) },
                        colors = if (option == sortOption) {
                            AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        } else {
                            AssistChipDefaults.assistChipColors()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockDetails(
    block: FormationBlock,
    courses: List<FormationCourse>,
    onCourseSelected: (CourseRef) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val creditWidth = remember(maxWidth) {
            when {
                maxWidth < 400.dp -> 48.dp
                maxWidth < 560.dp -> 56.dp
                else -> baseCreditColumnWidth
            }
        }
        val periodWidth = remember(maxWidth) {
            when {
                maxWidth < 400.dp -> 88.dp
                maxWidth < 560.dp -> 108.dp
                else -> basePeriodColumnWidth
            }
        }
        val tableWidth = remember(maxWidth) { maxWidth }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .width(tableWidth),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                TableHeader(creditWidth, periodWidth)
                Spacer(Modifier.height(8.dp))
                courses.forEachIndexed { index, course ->
                    CourseRow(
                        course = course,
                        striped = index % 2 == 0,
                        creditWidth = creditWidth,
                        periodWidth = periodWidth,
                        onCourseSelected = onCourseSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun TableHeader(
    creditWidth: Dp,
    periodWidth: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Unite d'enseignement",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier.width(creditWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Credits",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        }
        Box(
            modifier = Modifier.width(periodWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Periodes",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CourseRow(
    course: FormationCourse,
    striped: Boolean,
    creditWidth: Dp,
    periodWidth: Dp,
    onCourseSelected: (CourseRef) -> Unit
) {
    val backgroundColor = if (striped) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .clickable { onCourseSelected(CourseRef(course.code, course.detailsUrl)) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = course.code,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier.width(creditWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = course.credits.formatCredits(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        val periodsLabel = course.periods
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf("-") }
            .joinToString(" - ")

        Box(
            modifier = Modifier.width(periodWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = periodsLabel,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
    Spacer(Modifier.height(6.dp))
}

private enum class SortOption { Default, CreditsAsc, CreditsDesc, Title }

private fun SortOption.label(): String = when (this) {
    SortOption.Default -> "Par defaut"
    SortOption.CreditsAsc -> "ECTS croissant"
    SortOption.CreditsDesc -> "ECTS decroissant"
    SortOption.Title -> "Titre A-Z"
}

private fun applySortAndFilter(
    courses: List<FormationCourse>,
    periodFilter: String,
    sortOption: SortOption
): List<FormationCourse> {
    var result = courses
    if (periodFilter != "Tous") {
        result = result.filter { course ->
            course.periods.any { it.equals(periodFilter, ignoreCase = true) }
        }
    }
    result = when (sortOption) {
        SortOption.CreditsAsc -> result.sortedBy { it.credits }
        SortOption.CreditsDesc -> result.sortedByDescending { it.credits }
        SortOption.Title -> result.sortedBy { it.title.lowercase() }
        SortOption.Default -> result
    }
    return result
}

private fun Double.formatCredits(): String {
    val rounded = (this * 100).roundToInt() / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

private val baseCreditColumnWidth = 70.dp
private val basePeriodColumnWidth = 120.dp
