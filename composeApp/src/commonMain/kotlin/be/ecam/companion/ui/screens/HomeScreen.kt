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
import androidx.compose.material.icons.filled.*
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
import be.ecam.companion.ui.components.BottomBar
import be.ecam.companion.ui.components.BottomItem
import be.ecam.companion.viewmodel.HomeViewModel
import be.ecam.companion.viewmodel.LoginViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel(),
    loginViewModel: LoginViewModel
    
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBottomItem by remember { mutableStateOf(BottomItem.DASHBOARD) }
    val user = loginViewModel.currentUser

    if (user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Utilisateur non connecté")
        }
        return
    }

    LaunchedEffect(user.id) { vm.load(user) }

    val displayName = buildString {
        listOfNotNull(user.firstName, user.lastName)
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
            ?.let { append(it) }
        if (isEmpty()) append(user.username.split(" ").firstOrNull().orEmpty())
        if (isEmpty()) append("Étudiant")
    }

    val displayedCourses = remember(vm.courses, vm.catalogCourses, searchQuery) {
        if (searchQuery.isBlank()) vm.courses
        else vm.catalogCourses.filter {
            it.title.contains(searchQuery, true) || it.code.contains(searchQuery, true)
        }.map {
            PaeCourse(code = it.code, title = it.title, ects = it.credits?.toIntOrNull() ?: 0)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        AnimatedContent(
            targetState = vm.selectedCourseForResources,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "Navigation"
        ) { selectedCourse ->

            if (selectedCourse == null) {
                // 🎯 ÉCRAN D’ACCUEIL
                HomeMainScreen(
                    displayName = displayName,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    displayedCourses = displayedCourses,
                    onCourseClick = {
                        vm.openCourseResources(it)
                        selectedBottomItem = BottomItem.DASHBOARD
                    }
                )

            } else {
                // 🎯 ÉCRAN RESSOURCES
                CoursesResourcesScreen(
                    courseCode = selectedCourse.code ?: "",
                    courseTitle = selectedCourse.title ?: "",
                    onBack = { vm.closeCourseResources() }
                )
            }
        }

        // 🎯 BOTTOM BAR
        BottomBar(
            selected = selectedBottomItem,
            onSelect = { item ->
                selectedBottomItem = item

                when (item) {
                    BottomItem.DASHBOARD -> vm.closeCourseResources()
                    else -> {} // autres onglets si ajoutés plus tard
                }
            }
        )
    }
}

@Composable
fun HomeMainScreen(
    displayName: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    displayedCourses: List<PaeCourse>,
    onCourseClick: (PaeCourse) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text(
            text = if (searchQuery.isBlank()) "Bonjour, $displayName" else "Recherche",
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
            else "Résultats (${displayedCourses.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(displayedCourses) { course ->
                CourseCard(course = course, onClick = { onCourseClick(course) })
            }
        }
    }
}

@Composable
fun CourseCard(course: PaeCourse, onClick: () -> Unit) {
    val (color, icon) = getCourseTheme(course.title ?: "")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(color.copy(.15f), color.copy(.05f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(60.dp))
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(course.title ?: "", fontWeight = FontWeight.Bold)
                Text(course.code ?: "", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

private fun getCourseTheme(title: String): Pair<Color, ImageVector> {
    val t = title.lowercase()
    return when {
        "mobile" in t || "android" in t || "java" in t -> Color(0xFF4CAF50) to Icons.Filled.Phone
        "web" in t || "architecture" in t -> Color(0xFFE91E63) to Icons.Filled.Public
        "data" in t || "base" in t -> Color(0xFF2196F3) to Icons.Filled.Star
        "network" in t -> Color(0xFFFF9800) to Icons.Filled.Share
        "security" in t -> Color(0xFFF44336) to Icons.Filled.Lock
        "gpu" in t -> Color(0xFF607D8B) to Icons.Filled.Build
        else -> Color(0xFF607D8B) to Icons.Filled.School
    }
}
