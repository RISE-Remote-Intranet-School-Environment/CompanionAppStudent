package be.ecam.companion.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
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
import kotlin.math.absoluteValue
import be.ecam.companion.data.PaeCourse
import be.ecam.companion.ui.components.BottomBar
import be.ecam.companion.ui.components.BottomItem
import be.ecam.companion.viewmodel.HomeViewModel
import be.ecam.companion.viewmodel.LoginViewModel
import kotlinx.coroutines.delay

private fun normalizeCode(code: String?): String =
    code.orEmpty().trim().lowercase().replace(" ", "")

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    vm: HomeViewModel,
    loginViewModel: LoginViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBottomItem by remember { mutableStateOf(BottomItem.DASHBOARD) }
    val user = loginViewModel.currentUser

    // Snackbar pour le feedback
    val snackbarHostState = remember { SnackbarHostState() }

    // Afficher le message de feedback
    LaunchedEffect(vm.feedbackMessage) {
        vm.feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            delay(1500)
            vm.clearFeedback()
        }
    }

    if (user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Utilisateur non connecté")
        }
        return
    }

    LaunchedEffect(user.id, loginViewModel.jwtToken) { vm.load(user, loginViewModel.jwtToken) }

    val displayName = buildString {
        user.firstName?.takeIf { it.isNotBlank() }?.let { 
            append(it) 
        } ?: run {
            append(user.username.split(" ").firstOrNull().orEmpty())
        }
        if (isEmpty()) append("Étudiant")
    }

    val catalogIndex = remember(vm.catalogCourses) {
        vm.catalogCourses.associateBy { normalizeCode(it.code) }
    }

    // Mode recherche
    val isSearching = searchQuery.isNotBlank()

    // Liste des cours à afficher
    val displayedItems: List<CourseDisplayItem> = remember(vm.courses, vm.catalogCourses, searchQuery, vm.myCourseIds) {
        if (!isSearching) {
            // Mode normal : afficher les cours inscrits
            vm.courses.map { course ->
                //  CORRECTION : Toujours chercher dans le catalogue pour enrichir les titres
                val normalizedCode = normalizeCode(course.code)
                val match = catalogIndex[normalizedCode]
                CourseDisplayItem(
                    code = course.code ?: "",
                    //  Priorité : titre du cours > titre du catalogue > code
                    title = course.title?.takeIf { it.isNotBlank() && it != course.code } 
                        ?: match?.title 
                        ?: course.code 
                        ?: "",
                    ects = course.ects ?: match?.credits?.toIntOrNull(),
                    isSelected = true,
                    isFromCatalog = false
                )
            }
        } else {
            // Mode recherche : afficher les résultats du catalogue
            vm.catalogCourses
                .filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.code.contains(searchQuery, ignoreCase = true)
                }
                .map { course ->
                    CourseDisplayItem(
                        code = course.code,
                        title = course.title,
                        ects = course.credits?.toIntOrNull(),
                        isSelected = vm.isCourseSelected(course.code),
                        isFromCatalog = true
                    )
                }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = modifier.fillMaxSize().padding(paddingValues)) {

            AnimatedContent(
                targetState = vm.selectedCourseForResources,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "Navigation"
            ) { selectedCourse ->

                if (selectedCourse == null) {
                    // 🎯 ÉCRAN D'ACCUEIL
                    HomeMainScreen(
                        displayName = displayName,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        displayedItems = displayedItems,
                        isSearching = isSearching,
                        isAddingCourse = vm.isAddingCourse,
                        onCourseClick = { item ->
                            if (!item.isFromCatalog) {
                                vm.openCourseResources(PaeCourse(item.code, item.title, item.ects))
                            }
                        },
                        onAddCourse = { code -> vm.addCourse(code) },
                        onRemoveCourse = { code -> vm.removeCourse(code) }
                    )

                } else {
                    // ÉCRAN RESSOURCES
                    CoursesResourcesScreen(
                        courseCode = selectedCourse.code ?: "",
                        courseTitle = selectedCourse.title ?: "",
                        onBack = { vm.closeCourseResources() },
                        authToken = loginViewModel.jwtToken
                    )
                }
            }

            // BOTTOM BAR
            BottomBar(
                selected = selectedBottomItem,
                onSelect = { item ->
                    selectedBottomItem = item
                    when (item) {
                        BottomItem.DASHBOARD -> vm.closeCourseResources()
                        else -> {}
                    }
                }
            )
        }
    }
}

// Data class pour l'affichage unifié des cours
private data class CourseDisplayItem(
    val code: String,
    val title: String,
    val ects: Int?,
    val isSelected: Boolean,
    val isFromCatalog: Boolean
)

@Composable
private fun HomeMainScreen(
    displayName: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    displayedItems: List<CourseDisplayItem>,
    isSearching: Boolean,
    isAddingCourse: Boolean,
    onCourseClick: (CourseDisplayItem) -> Unit,
    onAddCourse: (String) -> Unit,
    onRemoveCourse: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text(
            //  CORRECTION : Virgule APRÈS le prénom
            text = if (searchQuery.isBlank()) "Bonjour $displayName," else "Recherche dans le catalogue",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Rechercher dans tout le catalogue...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Clear, "Effacer")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isBlank()) "Mes espaces de cours"
            else "Résultats (${displayedItems.size}) - Cliquez sur + pour ajouter",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(8.dp))

        if (displayedItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank())
                        "Aucun cours inscrit. Utilisez la recherche pour ajouter des cours."
                    else
                        "Aucun cours trouvé pour \"$searchQuery\"",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayedItems, key = { it.code }) { item ->
                    CourseCard(
                        item = item,
                        isSearching = isSearching,
                        isLoading = isAddingCourse,
                        onClick = { onCourseClick(item) },
                        onAdd = { onAddCourse(item.code) },
                        onRemove = { onRemoveCourse(item.code) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseCard(
    item: CourseDisplayItem,
    isSearching: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val theme = getCourseTheme(item.title, item.code)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(enabled = !isSearching) { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Zone avec icône et dégradé
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(theme.color.copy(.20f), theme.color.copy(.08f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(theme.icon, null, tint = theme.color, modifier = Modifier.size(64.dp))
                }

                // Infos du cours
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        item.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.code,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        item.ects?.let { ects ->
                            Text(
                                "• $ects ECTS",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            //  Bouton d'ajout/suppression (visible en mode recherche)
            if (isSearching) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = { if (item.isSelected) onRemove() else onAdd() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (item.isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (item.isSelected) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = if (item.isSelected) "Retirer" else "Ajouter",
                                tint = if (item.isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Badge "Ajouté" si le cours est sélectionné en mode recherche
            if (isSearching && item.isSelected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "Ajouté",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

private data class CourseTheme(val color: Color, val icon: ImageVector)

private fun getCourseTheme(title: String, code: String): CourseTheme {
    val palette = listOf(
        Color(0xFFE57373),
        Color(0xFFF06292),
        Color(0xFFBA68C8),
        Color(0xFF64B5F6),
        Color(0xFF4DD0E1),
        Color(0xFF81C784),
        Color(0xFFFFD54F),
        Color(0xFFFFB74D),
        Color(0xFFA1887F),
        Color(0xFF90A4AE)
    )

    val idx = (code + title).hashCode().absoluteValue % palette.size
    val color = palette[idx]

    val t = title.lowercase()
    val icon = when {
        "mobile" in t || "android" in t || "java" in t -> Icons.Filled.Phone
        "web" in t || "architecture" in t -> Icons.Filled.Public
        "data" in t || "base" in t -> Icons.Filled.Star
        "network" in t -> Icons.Filled.Share
        "security" in t -> Icons.Filled.Lock
        "gpu" in t || "compute" in t -> Icons.Filled.Build
        else -> Icons.Filled.School
    }

    return CourseTheme(color = color, icon = icon)
}