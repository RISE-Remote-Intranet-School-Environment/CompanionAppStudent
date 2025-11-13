package be.ecam.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.serialization.Serializable
import androidx.compose.runtime.*
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.resource


// --- Structures de données simples (tu les remplaceras plus tard par ton JSON) ---
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
@OptIn(
    org.jetbrains.compose.resources.ExperimentalResourceApi::class,
    kotlinx.serialization.ExperimentalSerializationApi::class
)
@Composable
fun LoadCourses(): List<CourseDetail> {
    var courses by remember { mutableStateOf<List<CourseDetail>>(emptyList()) }

    LaunchedEffect(Unit) {
        // 1️⃣ Lecture du fichier JSON depuis les ressources
        val jsonText = resource("files/ecam_courses_details_2025.json")
            .readBytes()
            .decodeToString()

        // 2️⃣ Configuration du parseur JSON
        val json = Json {
            ignoreUnknownKeys = true  // ignore les champs inconnus
            isLenient = true          // tolère les petits écarts de format
            prettyPrint = false
            namingStrategy = JsonNamingStrategy.SnakeCase // 🔥 convertit snake_case <-> camelCase
        }

        // 3️⃣ Décodage vers ta liste de cours
        courses = json.decodeFromString<List<CourseDetail>>(jsonText)
    }

    return courses
}

@Composable
fun CourseDetailScreen(course: CourseDetail) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // --- En-tête du cours ---
        Text(
            text = "${course.code} - ${course.title}",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (!course.bloc.isNullOrBlank() || !course.program.isNullOrBlank()) {
            Text("Bloc : ${course.bloc ?: "-"} | Programme : ${course.program ?: "-"}")
        }
        Text("Crédits : ${course.credits ?: "-"} | Heures : ${course.hours ?: "-"}")
        Text("Responsable : ${course.responsable ?: "-"}")
        Text("Langue : ${course.language ?: "-"}")

        Text(
            text = if (course.mandatory) "Unité obligatoire" else "Unité optionnelle",
            color = if (course.mandatory) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(20.dp))

        // --- Activités organisées ---
        if (course.organized_activities.isNotEmpty()) {
            SectionTitle("Activités organisées")
            course.organized_activities.forEach { act ->
                ActivityCard(act)
            }
            Spacer(Modifier.height(16.dp))
        }

        // --- Activités évaluées ---
        if (course.evaluated_activities.isNotEmpty()) {
            SectionTitle("Activités évaluées")
            course.evaluated_activities.forEach { eval ->
                EvaluationCard(eval)
            }
            Spacer(Modifier.height(16.dp))
        }

        // --- Sections textuelles (Contribution, Contenu, Méthodes...) ---
        if (course.sections.isNotEmpty()) {
            course.sections.forEach { (title, content) ->
                SectionTitle(title)
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        } else {
            Text(
                "Aucune information complémentaire disponible.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun CoursesFicheScreen(courseCode: String, onBack: () -> Unit) {
    val allCourses = rememberCoursesDetails()
    val course = allCourses.find { it.code.equals(courseCode, ignoreCase = true) }

    if (course == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedButton(onClick = onBack) { Text("← Retour") }
            Spacer(Modifier.height(8.dp))
            Text("Fiche introuvable pour le cours $courseCode")
        }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            OutlinedButton(onClick = onBack) { Text("← Retour") }
            Spacer(Modifier.height(16.dp))
            CourseDetailScreen(course = course)
        }
    }
}
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun ActivityCard(activity: OrganizedActivity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(activity.title, fontWeight = FontWeight.Bold)
            Text("Code : ${activity.code}")
            Text("Q1 : ${activity.hours_Q1 ?: "-"} | Q2 : ${activity.hours_Q2 ?: "-"}")
            if (activity.teachers.isNotEmpty()) {
                Text("Enseignants : ${activity.teachers.joinToString(", ")}")
            }
            Text("Langue : ${activity.language ?: "-"}")
        }
    }
}

@Composable
fun EvaluationCard(eval: EvaluatedActivity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(eval.title, fontWeight = FontWeight.Bold)
            Text("Code : ${eval.code}")
            if (!eval.weight.isNullOrBlank()) Text("Pondération : ${eval.weight}%")
            if (!eval.type_Q1.isNullOrBlank()) Text("Évaluation Q1 : ${eval.type_Q1}")
            if (!eval.type_Q2.isNullOrBlank()) Text("Évaluation Q2 : ${eval.type_Q2}")
            if (!eval.type_Q3.isNullOrBlank()) Text("Évaluation Q3 : ${eval.type_Q3}")
            if (eval.teachers.isNotEmpty()) Text("Enseignants : ${eval.teachers.joinToString(", ")}")
            if (eval.linked_activities.isNotEmpty()) {
                Text("Activités liées : ${eval.linked_activities.joinToString(" / ")}")
            }
        }
    }
}
