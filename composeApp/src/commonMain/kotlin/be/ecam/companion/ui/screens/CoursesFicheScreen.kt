@file:Suppress("DEPRECATION")

package be.ecam.companion.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import companion.composeapp.generated.resources.Res
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi




@Serializable
data class OrganizedActivity(
    val code: String,
    val title: String,
    val hours_Q1: String? = null,
    val hours_Q2: String? = null,
    val teachers: List<String> = emptyList(),
    val language: String? = null
)

@Serializable
data class EvaluatedActivity(
    val code: String,
    val title: String,
    val weight: String? = null,
    val type_Q1: String? = null,
    val type_Q2: String? = null,
    val type_Q3: String? = null,
    val teachers: List<String> = emptyList(),
    val language: String? = null,
    val linked_activities: List<String> = emptyList()
)

@Serializable
data class CourseDetail(
    val code: String,
    val title: String,
    val credits: String? = null,
    val hours: String? = null,
    val mandatory: Boolean = false,
    val bloc: String? = null,
    val program: String? = null,
    val responsable: String? = null,
    val language: String? = null,
    val organized_activities: List<OrganizedActivity> = emptyList(),
    val evaluated_activities: List<EvaluatedActivity> = emptyList(),
    val sections: Map<String, String> = emptyMap()
)

data class CourseRef(val code: String, val detailsUrl: String?)




@OptIn(ExperimentalResourceApi::class)
@Composable
fun loadCourses(): List<CourseDetail> {
    val state = produceState(initialValue = emptyList<CourseDetail>()) {
        try {
            val bytes = Res.readBytes("files/ecam_courses_details_2025.json")
            if (bytes.isNotEmpty()) {
                val json = Json { ignoreUnknownKeys = true; isLenient = true }
                value = json.decodeFromString(bytes.decodeToString())
            }
        } catch (e: Throwable) {
            println("[CoursesLoader] error: ${e.message}")
            value = emptyList()
        }
    }
    return state.value
}


@Composable
fun rememberCoursesDetails(): List<CourseDetail> = loadCourses()
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesFicheScreen(
    courseRef: CourseRef,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allCourses = rememberCoursesDetails()
    val normalizedKey = courseRef.code.lowercase().replace(" ", "")

    val course = allCourses.find { cd ->
        cd.code.lowercase().replace(" ", "").contains(normalizedKey)
                || cd.title.lowercase().replace(" ", "").contains(normalizedKey)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                title = {
                    Text(
                        text = course?.title ?: courseRef.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (course == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (allCourses.isEmpty()) {
                    CircularProgressIndicator()
                } else {
                    Text("Aucune fiche trouvée pour '${courseRef.code}'.")
                }
            }
        } else {
            CourseDetailScreen(
                course = course,
                modifier = Modifier.padding(padding)
            )
        }
    }
}


@Composable
fun CourseDetailScreen(course: CourseDetail, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "${course.code} — ${course.title}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(Modifier.height(8.dp))

                Text("Bloc : ${course.bloc ?: "-"}")
                Text("Programme : ${course.program ?: "-"}")
                Text("Crédits : ${course.credits ?: "-"} | Heures : ${course.hours ?: "-"}")
                Text("Responsable : ${course.responsable ?: "-"}")
                Text("Langue : ${course.language ?: "-"}")

                Spacer(Modifier.height(6.dp))

                AssistChip(
                    onClick = {},
                    label = {
                        Text(if (course.mandatory) "Obligatoire" else "Optionnel")
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (course.organized_activities.isNotEmpty()) {
            SectionTitle("Activités organisées", Icons.Default.List)
            OrganizedActivitiesTable(course.organized_activities)
        }

        Spacer(Modifier.height(16.dp))

        if (course.evaluated_activities.isNotEmpty()) {
            SectionTitle("Activités évaluées", Icons.Default.Info)
            EvaluatedActivitiesTable(course.evaluated_activities)
        }

        Spacer(Modifier.height(16.dp))

        if (course.sections.isNotEmpty()) {
            SectionsResponsiveLayout(course.sections)
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun OrganizedActivitiesTable(activities: List<OrganizedActivity>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Code", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text("Activité", Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text("Heures\n Q1/ Q2", Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                Text("Prof.", Modifier.weight(1.5f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }

            HorizontalDivider()

            activities.forEach { act ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(act.code, Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall)
                    Text(act.title, Modifier.weight(2f), style = MaterialTheme.typography.bodySmall)
                    val q1 = act.hours_Q1 ?: "-"
                    val q2 = act.hours_Q2 ?: "-"
                    Text("$q1 / $q2   ", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    Text(act.teachers.joinToString(", "), Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun EvaluatedActivitiesTable(list: List<EvaluatedActivity>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text("Activité", Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text("Ponderation", Modifier.weight(0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                Text("Q1/Q2/Q3", Modifier.weight(1.2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }

            HorizontalDivider()

            list.forEach { eval ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(2f)) {
                        Text(eval.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(eval.code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Text(if(eval.weight != null) "${eval.weight}%" else "-", Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    val q1 = eval.type_Q1 ?: "-"
                    val q2 = eval.type_Q2 ?: "-"
                    val q3 = eval.type_Q3 ?: "-"
                    Text("$q1  /  $q2   /  $q3", Modifier.weight(1.8f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun ExpandableSectionCard(
    title: String,
    content: String
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.clickable { expanded = !expanded }.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )

                Icon(
                    imageVector = if (expanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }
    }
}

@Composable
fun isWideScreen(): Boolean {
    var wide by remember { mutableStateOf(false) }
    BoxWithConstraints {
        wide = maxWidth > 900.dp
    }
    return wide
}

@Composable
fun SectionsResponsiveLayout(sections: Map<String, String>) {
    val wide = isWideScreen()

    if (wide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sections.entries.filterIndexed { index, _ -> index % 2 == 0 }
                    .forEach { (title, content) ->
                        ExpandableSectionCard(title, content)
                    }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sections.entries.filterIndexed { index, _ -> index % 2 == 1 }
                    .forEach { (title, content) ->
                        ExpandableSectionCard(title, content)
                    }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sections.forEach { (title, content) ->
                ExpandableSectionCard(title, content)
            }
        }
    }
}


