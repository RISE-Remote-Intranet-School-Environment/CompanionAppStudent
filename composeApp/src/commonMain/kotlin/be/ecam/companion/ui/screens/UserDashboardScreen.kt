package be.ecam.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import be.ecam.companion.viewmodel.LoginViewModel

@Composable
fun UserDashboardScreen(
    loginViewModel: LoginViewModel,
    modifier: Modifier = Modifier
) {
    val user = loginViewModel.currentUser

    if (user == null) {
        // En attente ou non connecté
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Aucune donnée utilisateur.")
        }
        return
    }

    val scrollState = rememberScrollState()

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.username.first().toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Bienvenue, ${user.username}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text("Email : ${user.email}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Informations personnelles", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("ID : ${user.id}")
                    Text("Email : ${user.email}")
                }
            }
        }
    }
}

// Modèle mock
data class MockUser(
    val id: Int,
    val username: String,
    val email: String,
    val role: String,
    val permissions: List<String>
)
