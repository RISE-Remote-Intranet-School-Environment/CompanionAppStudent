package be.ecam.companion.ui


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CoursesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Liste des cours",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(24.dp))

        // Ici tu pourras plus tard mettre ta liste de cours
        Text("Aucun cours pour le moment.")
    }
}
