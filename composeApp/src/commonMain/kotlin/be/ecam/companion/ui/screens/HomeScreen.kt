package be.ecam.companion.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ecam.companion.data.PaeCourse
import be.ecam.companion.ui.CourseDetail
import be.ecam.companion.ui.CourseRef
import be.ecam.companion.ui.CoursesFicheScreen
import be.ecam.companion.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel(),
    currentUser: String
) {
    var selectedCourseRef by remember { mutableStateOf<CourseRef?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(currentUser) {
        vm.load(currentUser)
    }

    val displayName = remember(vm.student, currentUser) {
        vm.student?.studentName?.takeIf { it.isNotBlank() } ?: currentUser
    }

    // --- LOGIQUE DE RECHERCHE GLOBALE ---
    // Si recherche vide -> On affiche "vm.courses" (Mes cours inscrits)
    // Si recherche active -> On cherche dans "vm.catalogCourses" (Tout le catalogue)
    val displayedCourses = remember(vm.courses, vm.catalogCourses, searchQuery) {
        if (searchQuery.isBlank()) {
            vm.courses
        } else {
            // Filtrage dans le catalogue complet
            val results = vm.catalogCourses.filter { detail ->
                detail.title.contains(searchQuery, ignoreCase = true) ||
                        detail.code.contains(searchQuery, ignoreCase = true)
            }
            // Conversion CourseDetail -> PaeCourse pour l'affichage
            results.map { detail ->
                PaeCourse(
                    code = detail.code,
                    title = detail.title,
                    ects = detail.credits?.toIntOrNull() ?: 0
                )
            }
        }
    }

    AnimatedContent(
        targetState = selectedCourseRef,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "HomeNavigation"
    ) { currentSelection ->

        if (currentSelection != null) {
            CoursesFicheScreen(
                courseRef = currentSelection,
                onBack = { selectedCourseRef = null },
                modifier = modifier
            )
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // TITRE (Change selon le mode)
                Text(
                    text = if (searchQuery.isBlank()) "Bonjour, $displayName" else "Recherche",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                // BARRE DE RECHERCHE
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Rechercher dans tout le catalogue...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                // SOUS-TITRE
                Text(
                    text = if (searchQuery.isBlank()) "Mes espaces de cours" else "Résultats globaux (${displayedCourses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(Modifier.height(8.dp))

                // GRILLE
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (searchQuery.isBlank() && vm.courses.isEmpty()) {
                        Box(contentAlignment = Alignment.Center) {
                            if (vm.student == null) CircularProgressIndicator()
                            else Text("Aucun cours inscrit.")
                        }
                    } else if (searchQuery.isNotBlank() && displayedCourses.isEmpty()) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Aucun résultat trouvé dans le catalogue.")
                        }
                    } else {
                        LazyVerticalGrid(
                            // 280.dp pour garder les cartes larges (style Claco)
                            columns = GridCells.Adaptive(minSize = 280.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(displayedCourses) { course ->
                                CourseCard(
                                    course = course,
                                    onClick = {
                                        val code = course.code ?: ""
                                        selectedCourseRef = CourseRef(code = code, detailsUrl = null)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CourseCard(
    course: PaeCourse,
    onClick: () -> Unit
) {
    val theme = remember(course.title) { getCourseTheme(course.title ?: "") }
    val cardColor = theme.first
    val cardIcon = theme.second

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(cardColor.copy(alpha = 0.15f), cardColor.copy(alpha = 0.05f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = cardIcon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = cardColor
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${course.ects ?: 0} ECTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = course.title ?: "Cours sans titre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = course.code?.uppercase() ?: "----",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

private fun getCourseTheme(title: String): Pair<Color, ImageVector> {
    val lower = title.lowercase()
    return when {
        "mobile" in lower || "android" in lower || "java" in lower -> Color(0xFF4CAF50) to Icons.Filled.Phone
        "web" in lower || "architecture" in lower -> Color(0xFFE91E63) to Icons.Filled.Public
        "data" in lower || "base" in lower -> Color(0xFF2196F3) to Icons.Filled.Star
        "intelligence" in lower || "ai" in lower -> Color(0xFF9C27B0) to Icons.Filled.Person
        "network" in lower || "réseau" in lower -> Color(0xFFFF9800) to Icons.Filled.Share
        "security" in lower || "sécurité" in lower -> Color(0xFFF44336) to Icons.Filled.Lock
        "gpu" in lower || "chip" in lower || "system" in lower -> Color(0xFF607D8B) to Icons.Filled.Build
        else -> Color(0xFF607D8B) to Icons.Filled.School
    }
}