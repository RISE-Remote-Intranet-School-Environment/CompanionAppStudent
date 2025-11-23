package be.ecam.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import companion.composeapp.generated.resources.Res



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
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                title = { Text(course?.title ?: "Fiche de cours") }
            )
        }
    ) { padding ->
        if (course == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucune fiche trouvée pour '${courseRef.code}'.")
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


        course.sections.forEach { (title, content) ->
            SectionCard(title) {
                Text(
                    content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(12.dp))
        }
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
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun ActivityCard(activity: OrganizedActivity) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(activity.title, fontWeight = FontWeight.Bold)
            Text("Code : ${activity.code}")
            Text("Q1 : ${activity.hours_Q1 ?: "-"} | Q2 : ${activity.hours_Q2 ?: "-"}")
            if (activity.teachers.isNotEmpty())
                Text("Enseignants : ${activity.teachers.joinToString(", ")}")
            Text("Langue : ${activity.language ?: "-"}")
        }
    }
}

@Composable
fun EvaluationCard(eval: EvaluatedActivity) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(eval.title, fontWeight = FontWeight.Bold)
            Text("Code : ${eval.code}")
            eval.weight?.let { Text("Pondération : $it%") }
            eval.type_Q1?.let { Text("Évaluation Q1 : $it") }
            eval.type_Q2?.let { Text("Évaluation Q2 : $it") }
            eval.type_Q3?.let { Text("Évaluation Q3 : $it") }
            if (eval.teachers.isNotEmpty())
                Text("Enseignants : ${eval.teachers.joinToString(", ")}")
            if (eval.linked_activities.isNotEmpty())
                Text("Activités liées : ${eval.linked_activities.joinToString(" / ")}")
        }
    }
}

@Composable
fun rememberCoursesDetails(): List<CourseDetail> = loadCourses()
@Composable
fun OrganizedActivitiesTable(activities: List<OrganizedActivity>) {

    Column(Modifier.fillMaxWidth()) {

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Code", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Activité", Modifier.weight(2f), fontWeight = FontWeight.Bold)
            Text("Heures", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Enseignants", Modifier.weight(2f), fontWeight = FontWeight.Bold)
        }

        Divider()

        activities.forEach { act ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text(act.code, Modifier.weight(1f))
                Text(act.title, Modifier.weight(2f))
                Text("${act.hours_Q1 ?: "-"} / ${act.hours_Q2 ?: "-"}", Modifier.weight(1f))
                Text(act.teachers.joinToString(", "), Modifier.weight(2f))
            }
            Divider()
        }
    }
}

@Composable
fun EvaluatedActivitiesTable(list: List<EvaluatedActivity>) {

    Column(Modifier.fillMaxWidth()) {

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Code", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Activité", Modifier.weight(2f), fontWeight = FontWeight.Bold)
            Text("%", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Q1", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Q2", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Q3", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Enseignants", Modifier.weight(2f), fontWeight = FontWeight.Bold)
        }

        Divider()

        list.forEach { eval ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(eval.code, Modifier.weight(1f))
                Text(eval.title, Modifier.weight(2f))
                Text(eval.weight ?: "-", Modifier.weight(1f))
                Text(eval.type_Q1 ?: "-", Modifier.weight(1f))
                Text(eval.type_Q2 ?: "-", Modifier.weight(1f))
                Text(eval.type_Q3 ?: "-", Modifier.weight(1f))
                Text(eval.teachers.joinToString(", "), Modifier.weight(2f))
            }
            Divider()
        }
    }
}
