package be.ecam.companion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
                            .weight(1f)
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
                        program = program,
                        block = block,
                        courses = filteredCourses,
                        onCourseSelected = onCourseSelected,
                        modifier = Modifier.weight(2.5f)
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
                    program = program,
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
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("Filtres et Tri", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Spacer(Modifier.height(14.dp))
            Text("Formation", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
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
                Spacer(Modifier.height(10.dp))
                Text("Bloc", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
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
            Spacer(Modifier.height(10.dp))
            Text("Periodes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
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
            Spacer(Modifier.height(10.dp))
            Text("Tri", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
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
    program: ProgramCardData,
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
                maxWidth < 400.dp -> 70.dp
                maxWidth < 560.dp -> 88.dp
                else -> basePeriodColumnWidth
            }
        }
        val tableWidth = remember(maxWidth) { maxWidth }
        val accentColor = formationAccentColor(program.formation.id, MaterialTheme.colorScheme.primary)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .width(tableWidth),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                TableHeader(program, block, courses.size, creditWidth, periodWidth, accentColor)
                Spacer(Modifier.height(10.dp))
                courses.forEachIndexed { index, course ->
                    CourseRow(
                        course = course,
                        striped = index % 2 == 0,
                        creditWidth = creditWidth,
                        periodWidth = periodWidth,
                        blockName = block.name,
                        accentColor = accentColor,
                        onCourseSelected = onCourseSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun TableHeader(
    program: ProgramCardData,
    block: FormationBlock,
    courseCount: Int,
    creditWidth: Dp,
    periodWidth: Dp,
    accentColor: Color
) {
    val headerTint = Brush.horizontalGradient(
        listOf(
            accentColor.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.surface
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BlocAvatar(block.name, size = 32.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${block.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$courseCount unité${if (courseCount > 1) "s" else ""} d'enseignement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormationAvatar(program.formation.id, size = 26.dp)
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = accentColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.ViewWeek,
                        contentDescription = null,
                        tint = accentColor
                    )
                    Text("Voir planning", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerTint, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Unité d'enseignement",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier.width(creditWidth),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Crédits",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
            Box(
                modifier = Modifier.width(periodWidth),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Périodes",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CourseRow(
    course: FormationCourse,
    striped: Boolean,
    creditWidth: Dp,
    periodWidth: Dp,
    blockName: String,
    accentColor: Color,
    onCourseSelected: (CourseRef) -> Unit
) {
    val backgroundColor = if (striped) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val stripeColor = accentColor.copy(alpha = 0.28f)
    val rowShape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(backgroundColor, rowShape)
            .drawBehind {
                val stripeWidth = 6.dp.toPx()
                drawRoundRect(
                    color = stripeColor,
                    topLeft = Offset.Zero,
                    size = Size(stripeWidth, size.height),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .padding(horizontal = 10.dp, vertical = 12.dp)
            .clickable { onCourseSelected(CourseRef(course.code, course.detailsUrl)) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            imageVector = courseIconForTitle(course.title),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
                .padding(end = 10.dp)
                .size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            val periodsLabel = course.periods
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf("-") }
                .joinToString(" - ")
            val metaLine = buildString {
                append(course.code)
                if (periodsLabel != "-") {
                    append(" - ")
                    append(periodsLabel)
                }
                append(" - ")
                append(blockName)
            }
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier.width(creditWidth),
            contentAlignment = Alignment.Center
        ) {
            StatChip(
                text = "${course.credits.formatCredits()} ECTS",
                color = creditColorFor(course.credits),
                modifier = Modifier.fillMaxWidth()
            )
        }

        val periodsLabel = course.periods
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf("-") }
            .joinToString(" - ")

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier.width(periodWidth),
            contentAlignment = Alignment.Center
        ) {
            StatChip(
                text = periodsLabel,
                color = periodColorFor(periodsLabel),
                modifier = Modifier.fillMaxWidth()
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

@Composable
private fun StatChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun creditColorFor(value: Double): Color = when {
    value >= 6 -> MaterialTheme.colorScheme.primary
    value >= 4 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.secondary
}

@Composable
private fun periodColorFor(label: String): Color = when {
    label.contains("Q1", ignoreCase = true) && label.contains("Q2", ignoreCase = true) -> MaterialTheme.colorScheme.primary
    label.contains("Q1", ignoreCase = true) -> Color(0xFF8D6E63)
    label.contains("Q2", ignoreCase = true) -> Color(0xFF26A69A)
    else -> MaterialTheme.colorScheme.outline
}

@Composable
private fun courseIconForTitle(title: String): ImageVector {
    val lower = title.lowercase()
    return when {
        listOf("chimie", "bio", "science").any { it in lower } -> Icons.Filled.Science
        listOf("electr", "energie", "circuit").any { it in lower } -> Icons.Filled.Bolt
        listOf("info", "communication", "reseau").any { it in lower } -> Icons.Filled.Computer
        listOf("mouvement", "physique", "mecanique").any { it in lower } -> Icons.Filled.Construction
        else -> Icons.Filled.School
    }
}

private fun blockAccentColor(blockName: String, fallback: Color): Color {
    val number = Regex("""\d+""").find(blockName)?.value
    return when (number) {
        "1" -> Color(0xFF7E57C2)
        "2" -> Color(0xFF5C6BC0)
        "3" -> Color(0xFF42A5F5)
        "4" -> Color(0xFF26A69A)
        "5" -> Color(0xFF66BB6A)
        "6" -> Color(0xFFF9A825)
        else -> fallback
    }
}

private fun formationAccentColor(formationId: String, fallback: Color): Color =
    when (formationId) {
        "automatisation" -> Color(0xFF5C6BC0)
        "construction" -> Color(0xFF8D6E63)
        "electromecanique" -> Color(0xFF00897B)
        "electronique" -> Color(0xFF7E57C2)
        "geometre" -> Color(0xFF5C7F67)
        "informatique" -> Color(0xFF3949AB)
        "ingenierie_sante" -> Color(0xFF26A69A)
        "ingenieur_industriel_commercial" -> Color(0xFFF9A825)
        "business_analyst" -> Color(0xFF6D4C41)
        else -> fallback
    }

private val baseCreditColumnWidth = 70.dp
private val basePeriodColumnWidth = 104.dp
