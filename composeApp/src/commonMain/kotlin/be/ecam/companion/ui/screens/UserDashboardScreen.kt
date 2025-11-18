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

@Composable
fun UserDashboardScreen(
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val userInfo = remember {
        if (isAdmin) {
            MockUser(
                id = 1,
                username = "AdminValatras",
                email = "admin@ecam.be",
                role = "Administrateur",
                permissions = listOf("Gérer utilisateurs", "Voir rapports", "Modifier offres")
            )
        } else {
            MockUser(
                id = 42,
                username = "EtudiantNicolas",
                email = "nicolas.etudiant@ecam.be",
                role = "Utilisateur",
                permissions = listOf("Voir offres", "Réserver sessions")
            )
        }
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
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInfo.username.first().toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Bienvenue, ${userInfo.username}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text("Rôle : ${userInfo.role}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Carte infos utilisateur
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Informations personnelles",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("ID : ${userInfo.id}")
                    Text("Email : ${userInfo.email}")
                    Text("Rôle : ${userInfo.role}")
                }
            }

            // Carte permissions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Permissions",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    userInfo.permissions.forEach { perm ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(perm)
                        }
                    }
                }
            }

            // Bouton admin
            if (isAdmin) {
                Button(
                    onClick = { /* navigation vers gestion utilisateurs */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Gérer les utilisateurs")
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
