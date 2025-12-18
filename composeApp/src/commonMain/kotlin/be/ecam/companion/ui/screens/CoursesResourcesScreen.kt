@file:OptIn(ExperimentalMaterial3Api::class)

package be.ecam.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import be.ecam.common.api.CourseResource
import be.ecam.common.api.CourseResourceRepository
import be.ecam.companion.data.CourseDetail
import be.ecam.companion.ui.CourseRef
import be.ecam.companion.ui.CoursesFicheScreen
import be.ecam.companion.ui.rememberCoursesDetails

@Composable
fun CoursesResourcesScreen(
    courseCode: String,
    courseTitle: String,
    onBack: () -> Unit,
    repository: CourseResourceRepository = CourseResourceRepository(),
    authToken: String? = null
) {
    var showFullFiche by remember { mutableStateOf(false) }
    if (showFullFiche) {
        CoursesFicheScreen(
            courseRef = CourseRef(code = courseCode, detailsUrl = null),
            authToken = authToken,
            onBack = { showFullFiche = false }
        )
        return
    }

    val resources by produceState(initialValue = emptyList<CourseResource>(), courseCode) {
        value = repository.getResourcesForCourse(courseCode)
    }
    val grouped = remember(resources) { resources.groupBy { it.type.lowercase() } }
    val uri = LocalUriHandler.current

    val detailsState = rememberCoursesDetails(authToken)
    val courseDetail = remember(detailsState.courses, courseCode) {
        val normalizedKey = courseCode.lowercase().replace(" ", "")
        detailsState.courses.find { detail ->
            detail.code.lowercase().replace(" ", "").contains(normalizedKey) ||
                    detail.title.lowercase().replace(" ", "").contains(normalizedKey)
        }
    }
    val summaryLines = remember(courseDetail, courseTitle) { buildCourseSummary(courseDetail, courseTitle) }
    val tipsLines = remember(courseDetail) { buildStudyTips(courseDetail) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = courseTitle,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                CourseIntroCard(
                    courseTitle = courseTitle,
                    courseDetail = courseDetail,
                    summaryLines = summaryLines,
                    onViewFullFiche = { showFullFiche = true }
                )
            }

            item {
                StudyTipsCard(tips = tipsLines)
            }

            grouped["announcement"]?.let { announcements ->
                item {
                    AnnouncementsSection(announcements = announcements, uri = uri)
                }
            }

            val orderedTypes = listOf("pdf", "video", "article", "image", "link")
            orderedTypes.forEach { type ->
                grouped[type]?.takeIf { it.isNotEmpty() }?.let { list ->
                    item {
                        ResourceSection(
                            title = resourceMeta(type).title,
                            icon = resourceMeta(type).icon,
                            accent = resourceMeta(type).color,
                            resources = list,
                            uri = uri
                        )
                    }
                }
            }

            val remainingTypes = grouped.keys
                .filterNot { it in orderedTypes || it == "announcement" }
                .sorted()
            remainingTypes.forEach { type ->
                grouped[type]?.let { list ->
                    item {
                        ResourceSection(
                            title = resourceMeta(type).title,
                            icon = resourceMeta(type).icon,
                            accent = resourceMeta(type).color,
                            resources = list,
                            uri = uri
                        )
                    }
                }
            }

            if (resources.isEmpty()) {
                item {
                    EmptyResourcesCard()
                }
            }
        }
    }
}

@Composable
private fun CourseIntroCard(
    courseTitle: String,
    courseDetail: CourseDetail?,
    summaryLines: List<String>,
    onViewFullFiche: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "Présentation du cours",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = courseTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            summaryLines.take(6).forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalButton(onClick = onViewFullFiche) {
                Icon(Icons.Filled.Book, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Voir la fiche UE complète")
            }
            if (courseDetail == null) {
                Text(
                    text = "Chargement de la fiche UE…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun StudyTipsCard(tips: List<String>) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "Comment étudier efficacement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            tips.forEach { tip ->
                Text(
                    text = "• $tip",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AnnouncementsSection(announcements: List<CourseResource>, uri: UriHandler) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "Annonces du professeur",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            announcements.forEach { announcement ->
                AnnouncementCard(resource = announcement, uri = uri)
            }
        }
    }
}

@Composable
private fun AnnouncementCard(resource: CourseResource, uri: UriHandler) {
    val currentUri by rememberUpdatedState(newValue = uri)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { currentUri.openUri(resource.url) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resource.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Annonce • Cliquer pour ouvrir",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun ResourceSection(
    title: String,
    icon: ImageVector,
    accent: Color,
    resources: List<CourseResource>,
    uri: UriHandler
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(resources) { res ->
                ResourceCard(res = res, accent = accent, icon = icon, uri = uri)
            }
        }
    }
}

@Composable
private fun ResourceCard(res: CourseResource, accent: Color, icon: ImageVector, uri: UriHandler) {
    ElevatedCard(
        modifier = Modifier
            .size(width = 260.dp, height = 120.dp)
            .clickable { uri.openUri(res.url) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = accent.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (res.type.lowercase()) {
                        "pdf" -> Icons.Filled.Description
                        "image" -> Icons.Filled.Image
                        "video" -> Icons.Filled.Movie
                        "article" -> Icons.AutoMirrored.Filled.Article
                        "link" -> Icons.Filled.Link
                        else -> icon
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.padding(horizontal = 6.dp))
                Text(
                    text = res.type.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
            Text(
                text = res.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(accent.copy(alpha = 0.4f))
                )
                Text(
                    text = "Ouvrir",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
        }
    }
}

@Composable
private fun EmptyResourcesCard() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Aucune ressource disponible pour l’instant.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Les supports seront ajoutés dès qu’ils seront publiés par l’enseignant.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class ResourceMeta(val title: String, val icon: ImageVector, val color: Color)

private fun resourceMeta(type: String): ResourceMeta = when (type.lowercase()) {
    "pdf" -> ResourceMeta("Documents PDF", Icons.Filled.Description, Color(0xFF1E88E5))
    "video" -> ResourceMeta("Vidéos et replays", Icons.Filled.Movie, Color(0xFFE91E63))
    "article" -> ResourceMeta("Articles et notes", Icons.AutoMirrored.Filled.Article, Color(0xFF43A047))
    "image" -> ResourceMeta("Images et schémas", Icons.Filled.Image, Color(0xFFFFA000))
    "link" -> ResourceMeta("Liens utiles", Icons.Filled.Link, Color(0xFF6A1B9A))
    "announcement" -> ResourceMeta("Annonces du professeur", Icons.Filled.Notifications, Color(0xFF00897B))
    else -> ResourceMeta("Autres ressources", Icons.Filled.Description, Color(0xFF5E35B1))
}

private fun buildCourseSummary(courseDetail: CourseDetail?, fallbackTitle: String): List<String> {
    if (courseDetail == null) {
        return listOf(
            "Fiche UE en cours de chargement.",
            "Ressources disponibles ci-dessous."
        )
    }
    val sections = courseDetail.sections
    val outcomes = sections["Acquis d’apprentissage spécifiques"]
        ?: sections["Acquis dƒ?Tapprentissage spÇ¸cifiques"]
    val contribution = sections["Contribution au programme"]
    val content = sections["Description du contenu"]
    val teaching = sections["Méthodes d'enseignement"]
    val evaluation = sections["Méthodes d'évaluation"]
    val lines = mutableListOf<String>()

    outcomes?.let { lines += "Compétences visées : ${cleanSnippet(it)}" }
    content?.let { lines += "Contenu principal : ${cleanSnippet(it)}" }
    contribution?.let { lines += "Objectif : ${cleanSnippet(it)}" }
    teaching?.let { lines += "Approche pédagogique : ${cleanSnippet(it)}" }
    evaluation?.let { lines += "Évaluation : ${cleanSnippet(it)}" }
    courseDetail.hours?.let { lines += "Volume horaire : $it" }
    courseDetail.credits?.let { lines += "Crédits : ${it} ECTS" }

    if (lines.isEmpty()) {
        lines += "Fiche UE : $fallbackTitle"
    }
    return lines.take(6)
}

private fun buildStudyTips(courseDetail: CourseDetail?): List<String> {
    val sections = courseDetail?.sections.orEmpty()
    val contentRaw = sections.values.joinToString(" ").lowercase()
    val evaluationRaw = (sections["Méthodes d'évaluation"] ?: "").lowercase()
    val teachingRaw = (sections["Méthodes d'enseignement"] ?: "").lowercase()
    val nature = inferCourseNature(courseDetail, contentRaw, teachingRaw)
    val tips = mutableListOf<String>()

    when (nature) {
        CourseNature.LAB -> {
            tips += "Prépare chaque labo en relisant la partie pratique avant la séance."
            tips += "Note les commandes ou gestes clés dès le premier passage pour gagner du temps aux évaluations."
        }
        CourseNature.PROJECT -> {
            tips += "Découpe le projet en jalons hebdomadaires et valide-les avec l’enseignant."
            tips += "Documente ton code et tes choix ; cela sert pour les soutenances."
        }
        CourseNature.THEORY -> {
            tips += "Refais les exercices type juste après le cours magistral pour ancrer les concepts."
            tips += "Construis une fiche mémo par chapitre pour réviser plus vite."
        }
        CourseNature.MIXED -> {
            tips += "Alterne lecture rapide du support et mise en pratique courte (quiz, mini-exo)."
            tips += "Planifie deux créneaux courts par semaine plutôt qu’une longue session."
        }
    }

    if ("continu" in evaluationRaw || "tp" in evaluationRaw) {
        tips += "Évaluation continue : rends chaque livrable dès sa semaine pour lisser la charge."
    }
    if ("examen" in evaluationRaw || "oral" in evaluationRaw || "écrit" in evaluationRaw) {
        tips += "Prépare-toi à l’examen : refais les sujets ou exemples fournis dans la fiche UE."
    }
    if ("groupe" in contentRaw || "équipe" in teachingRaw) {
        tips += "Travail en groupe : répartis clairement les rôles et consigne les décisions."
    }
    sections["Support de cours"]?.takeIf { it.isNotBlank() }?.let {
        tips += "Support conseillé : ${cleanSnippet(it)}"
    }

    return tips.take(6).ifEmpty { listOf("Consulte la fiche UE complète pour les modalités de travail.") }
}

private enum class CourseNature { LAB, PROJECT, THEORY, MIXED }

private fun inferCourseNature(detail: CourseDetail?, content: String, teaching: String): CourseNature {
    val text = listOf(detail?.title.orEmpty(), content, teaching).joinToString(" ").lowercase()
    return when {
        text.contains("labo") || text.contains("laboratoire") || text.contains("manip") -> CourseNature.LAB
        text.contains("projet") || text.contains("project") -> CourseNature.PROJECT
        text.contains("cours magistral") || text.contains("theorie") || text.contains("théorie") -> CourseNature.THEORY
        else -> CourseNature.MIXED
    }
}

private fun cleanSnippet(value: String): String =
    value
        .replace("\n", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(160)
        .removeSuffix(".")
