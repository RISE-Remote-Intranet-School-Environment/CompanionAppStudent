@file:Suppress("DEPRECATION")

package be.ecam.companion.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import be.ecam.companion.data.CourseDetail
import be.ecam.companion.data.CourseDetailsRepository
import be.ecam.companion.data.EvaluatedActivity
import be.ecam.companion.data.OrganizedActivity
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.buildBaseUrl
import io.ktor.client.HttpClient
import org.koin.compose.koinInject




data class CourseRef(val code: String, val detailsUrl: String?)

data class CourseDetailsLoadState(
    val courses: List<CourseDetail>,
    val isLoading: Boolean,
    val error: String?
)

@Composable
fun rememberCoursesDetails(authToken: String? = null): CourseDetailsLoadState {
    val httpClient = koinInject<HttpClient>()
    val settingsRepo = koinInject<SettingsRepository>()
    val host by settingsRepo.serverHostFlow.collectAsState(settingsRepo.getServerHost())
    val port by settingsRepo.serverPortFlow.collectAsState(settingsRepo.getServerPort())
    val bearerToken = remember(authToken) {
        authToken?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }
    }
    val repository = remember(httpClient, host, port, bearerToken) {
        CourseDetailsRepository(
            client = httpClient,
            baseUrlProvider = { buildBaseUrl(host, port) },
            authTokenProvider = { bearerToken }
        )
    }
    var error by remember(host, port, bearerToken) { mutableStateOf<String?>(null) }
    var loading by remember(host, port, bearerToken) { mutableStateOf(true) }
    val courses by produceState(initialValue = emptyList<CourseDetail>(), host, port, bearerToken) {
        loading = true
        error = null
        value = try {
            repository.loadAll()
        } catch (t: Throwable) {
            error = t.message?.takeIf { it.isNotBlank() } ?: t::class.simpleName ?: "Erreur inconnue"
            emptyList()
        }
        loading = false
    }
    return CourseDetailsLoadState(
        courses = courses,
        isLoading = loading,
        error = error
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesFicheScreen(
    courseRef: CourseRef,
    authToken: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detailsState = rememberCoursesDetails(authToken)
    val allCourses = detailsState.courses
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
                when {
                    detailsState.isLoading -> CircularProgressIndicator()
                    detailsState.error != null -> Text(
                        text = "Impossible de charger la fiche : ${detailsState.error}",
                        textAlign = TextAlign.Center
                    )
                    else -> Text("Aucune fiche trouvée pour '${courseRef.code}'.")
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
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Ponderation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Q1/Q2/Q3", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            }

            HorizontalDivider()

            list.forEach { eval ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(2f)) {
                        Text(eval.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(eval.code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if(eval.weight != null) "${eval.weight}%" else "-", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
                    val q1 = eval.type_Q1 ?: "-"
                    val q2 = eval.type_Q2 ?: "-"
                    val q3 = eval.type_Q3 ?: "-"
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$q1  /  $q2   /  $q3", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    }
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
