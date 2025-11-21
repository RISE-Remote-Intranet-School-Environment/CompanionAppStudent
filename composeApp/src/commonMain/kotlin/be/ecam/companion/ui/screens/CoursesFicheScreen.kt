package be.ecam.companion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import companion.composeapp.generated.resources.Res


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

// Petit DTO pour transmettre un identifiant de cours depuis `CoursesScreen`.
data class CourseRef(val code: String, val detailsUrl: String?)

@Composable
fun loadCourses(): List<CourseDetail> {
    // Chargement asynchrone via produceState car Res.readBytes est suspendante
    val state = produceState<List<CourseDetail>>(initialValue = emptyList()) {
        try {
            val bytes = Res.readBytes("files/ecam_courses_details_2025.json")
            if (bytes.isNotEmpty()) {
                val text = bytes.decodeToString()
                val json = Json { ignoreUnknownKeys = true; isLenient = true }
                val parsed = json.decodeFromString<List<CourseDetail>>(text)
                println("[CoursesLoader] loaded ${parsed.size} course fiches")
                value = parsed
            } else {
                value = emptyList()
            }
        } catch (t: Throwable) {
            // En cas d'erreur, on retourne liste vide
            println("[CoursesLoader] error loading courses: ${t.message}")
            value = emptyList()
        }
    }
    return state.value
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
fun CoursesFicheScreen(
    courseRef: CourseRef,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allCourses = rememberCoursesDetails()
    val query = courseRef.code.trim()

    fun normalizeKey(s: String): String {
        return s.replace(Regex("[^A-Za-z0-9]"), "").lowercase()
    }

    // Prépare une liste de valeurs candidates provenant du CourseRef
    val candidates = mutableListOf<String>().apply {
        add(query)
        courseRef.detailsUrl?.let { url ->
            // extraire le dernier segment du path (ex: https://.../1bach10 -> 1bach10)
            val last = url.trimEnd('/').substringAfterLast('/')
            if (last.isNotBlank()) add(last)
        }
    }

    val normalizedCandidates = candidates.map { normalizeKey(it) }.toSet()

    val course = allCourses.find { cd ->
        val nCode = normalizeKey(cd.code)
        val nTitle = normalizeKey(cd.title)
        // test direct contre tous les candidats
        if (normalizedCandidates.contains(nCode) || normalizedCandidates.contains(nTitle)) return@find true
        // tolerances
        if (normalizedCandidates.any { cand -> nCode.contains(cand) || nTitle.contains(cand) }) return@find true
        false
    }

    if (course == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedButton(onClick = onBack) { Text("← Retour") }
            Spacer(Modifier.height(8.dp))

            if (allCourses.isEmpty()) {
                // Chargement en cours
                Spacer(Modifier.height(12.dp))
                Text("Chargement des fiches...", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator()
            } else {
                Spacer(Modifier.height(12.dp))
                Text("Fiche introuvable pour le cours: \"${courseRef.code}\"", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Text("Fiches chargées: ${allCourses.size}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))

                // Affiche un échantillon de codes (premiers 30) pour debug
                val sample = allCourses.take(30).map { it.code }.joinToString(", ")
                Text("Exemples de codes: ", style = MaterialTheme.typography.titleSmall)
                Text(sample, style = MaterialTheme.typography.bodySmall, maxLines = 4)

                Spacer(Modifier.height(12.dp))
                Text("Query normalisée candidates: ${normalizedCandidates.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)

                // Montrer correspondances proches (codes dont le préfixe correspond)
                val close = allCourses.filter { c ->
                    val nk = normalizeKey(c.code)
                    normalizedCandidates.any { cand -> nk.startsWith(cand) || nk.contains(cand) }
                }.take(20).map { it.code }
                if (close.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Correspondances proches:", style = MaterialTheme.typography.titleSmall)
                    Text(close.joinToString(", "), style = MaterialTheme.typography.bodySmall, maxLines = 4)
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedButton(onClick = onBack) { Text("← Retour") }
            Spacer(Modifier.height(16.dp))
            CourseDetailScreen(course = course) // déjà scrollable à l’intérieur
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

@Composable
fun rememberCoursesDetails(): List<CourseDetail> {
    // Wrapper simple autour de `loadCourses()` pour rendre l'appel plus lisible
    // et centraliser le nom utilisé par le reste du code.
    return loadCourses()
}
