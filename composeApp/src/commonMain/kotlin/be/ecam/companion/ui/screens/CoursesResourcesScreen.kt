@file:OptIn(ExperimentalMaterial3Api::class)

package be.ecam.companion.ui.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import be.ecam.common.api.CourseResource
import be.ecam.common.api.CourseResourceRepository

@Composable
fun CoursesResourcesScreen(
    courseCode: String,
    courseTitle: String,
    onBack: () -> Unit,
    repository: CourseResourceRepository = CourseResourceRepository()
) {
    val resources by produceState(initialValue = emptyList<CourseResource>()) {
        value = repository.getResourcesForCourse(courseCode)
    }

    val grouped = remember(resources) { resources.groupBy { it.type.lowercase() } }
    val uri = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = courseTitle,
                        style = MaterialTheme.typography.titleLarge
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            grouped["pdf"]?.let { pdfs ->
                item { SectionHeader("Documents PDF", Icons.Default.Description, Color(0xFF2196F3)) }
                item { ResourceRow(pdfs, Color(0xFF2196F3), uri) }
            }

            grouped["article"]?.let { arts ->
                item { SectionHeader("Articles Web", Icons.AutoMirrored.Filled.Article, Color(0xFF4CAF50)) }
                item { ResourceRow(arts, Color(0xFF4CAF50), uri) }
            }

            grouped["image"]?.let { imgs ->
                item { SectionHeader("Images & Illustrations", Icons.Default.Image, Color(0xFFFFA726)) }
                item { ResourceRow(imgs, Color(0xFFFFA726), uri) }
            }

            grouped["video"]?.let { vids ->
                item { SectionHeader("Vidéos", Icons.Default.Movie, Color(0xFFE91E63)) }
                item { ResourceRow(vids, Color(0xFFE91E63), uri) }
            }
        }
    }
}


/* -------------------------------------------------------------------------- */
/*                               HEADER DE SECTION                             */
/* -------------------------------------------------------------------------- */

@Composable
fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}


/* -------------------------------------------------------------------------- */
/*                               LISTE HORIZONTALE                             */
/* -------------------------------------------------------------------------- */

@Composable
fun ResourceRow(list: List<CourseResource>, accent: Color, uri: androidx.compose.ui.platform.UriHandler) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(list) { res -> ResourceCardClean(res, accent, uri) }
    }
}


/* -------------------------------------------------------------------------- */
/*                                CARTE RESSOURCE                              */
/* -------------------------------------------------------------------------- */

@Composable
fun ResourceCardClean(res: CourseResource, accent: Color, uri: androidx.compose.ui.platform.UriHandler) {
    Card(
        modifier = Modifier
            .width(250.dp)
            .height(90.dp)
            .clickable { uri.openUri(res.url) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = when (res.type.lowercase()) {
                    "pdf" -> Icons.Default.Description
                    "image" -> Icons.Default.Image
                    "video" -> Icons.Default.Movie
                    else -> Icons.AutoMirrored.Filled.Article
                },
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(36.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = res.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Text(
                    text = res.type.uppercase(),
                    color = accent,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
