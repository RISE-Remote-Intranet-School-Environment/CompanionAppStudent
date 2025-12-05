package be.ecam.companion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import be.ecam.companion.data.PaeCourse
import androidx.compose.foundation.shape.RoundedCornerShape
import be.ecam.companion.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel()
) {

    LaunchedEffect(Unit) { vm.load() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp), // Marge globale
        verticalArrangement = Arrangement.Top
    ) {
        // En-tête avec nom de l'étudiant si disponible
        Text(
            text = if (vm.student?.studentName != null) "Bonjour, ${vm.student?.studentName}" else "Mes cours",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        if (vm.lastErrorMessage.isNotEmpty()) {
            Text(vm.lastErrorMessage, color = MaterialTheme.colorScheme.error)
        } else if (vm.courses.isEmpty()) {
            // Afficher un loader ou un message "vide" si aucun cours n'est trouvé
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if(vm.student == null) CircularProgressIndicator() else Text("Aucun cours trouvé pour 2025-2026")
            }
        } else {
            // --- C'EST ICI QUE LA MAGIE RESPONSIVE OPERE ---
            LazyVerticalGrid(
                // Adaptive : crée autant de colonnes que possible avec une largeur min de 180dp
                columns = GridCells.Adaptive(minSize = 180.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(vm.courses) { course ->
                    CourseCard(course)
                }
            }
        }
    }
}

@Composable
fun CourseCard(course: PaeCourse) {
    // On retire le width fixe ici car la Grid va gérer la largeur
    Card(
        modifier = Modifier
            .fillMaxWidth() // Prend toute la largeur de la colonne assignée
            .height(150.dp), // Hauteur fixe pour uniformité
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Code du cours (ex: 4eial40)
            Text(
                text = course.code?.uppercase() ?: "----",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            // Titre du cours
            Text(
                text = course.title ?: "Cours sans titre",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3, // Permet plus de texte sur les petits écrans
                overflow = TextOverflow.Ellipsis
            )

            // ECTS en bas
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = "${course.ects ?: 0} ECTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}