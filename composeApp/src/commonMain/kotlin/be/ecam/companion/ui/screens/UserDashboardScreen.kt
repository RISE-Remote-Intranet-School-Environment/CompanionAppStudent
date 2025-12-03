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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Aucune donnée utilisateur.")
        }
        return
    }

    val scrollState = rememberScrollState()

    // MOCKUP DATA
    val lastMessages = listOf(
        "Votre professeur a validé votre PAE.",
        "Nouvelle ressource ajoutée dans Mobile Dev.",
        "Mise à jour de l’horaire.",
        "Rappel : Projet Web pour le 12 déc.",
        "Notification de la vie étudiante."
    )

    val paeCourses = listOf(
        Triple("4eiai40", "Artificial Intelligence", "Q2"),
        Triple("4eial40", "Architecture and Software Quality", "Q1"),
        Triple("4eiam40", "Mobile Development", "Q2"),
        Triple("4eiaw40", "Web Architecture", "Q2"),
        Triple("4eidb40", "Database Management", "Q1"),
    )

    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditUserDialog(
            currentName = user.username,
            currentEmail = user.email,
            onDismiss = { showEditDialog = false },
            onSave = { newUsername, newEmail ->
                loginViewModel.updateMe(newUsername, newEmail)
                showEditDialog = false
            }
        )
    }


    // ————————————————————————————————————————————

    Surface(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ——————————————————
            // HEADER
            // ——————————————————
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.username.first().uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Bonjour, ${user.username}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Heureux de vous revoir 👋", color = Color.Gray)
                }
            }

            // ——————————————————
            // QUICK STATS (Petites cartes)
            // ——————————————————
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardMiniCard(
                    title = "ECTS",
                    value = "60",
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                DashboardMiniCard(
                    title = "Bloc",
                    value = "4",
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
                DashboardMiniCard(
                    title = "Cours",
                    value = paeCourses.size.toString(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // ——————————————————
            // DERNIERS MESSAGES — Medium Box
            // ——————————————————
            UserInfoCard(
                username = user.username,
                email = user.email,
                onEditClick = { showEditDialog = true }
            )

            DashboardSectionCard(
                title = "Derniers messages",
                content = {
                    lastMessages.forEach {
                        Text("• $it", modifier = Modifier.padding(vertical = 4.dp))
                    }
                },
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // ——————————————————
            // PAE — Large Box
            // ——————————————————
            DashboardSectionCard(
                title = "Votre PAE",
                subtitle = "Année académique 2025-2026 — Bloc 4 — Programme 4MIN",
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                paeCourses.forEach { (code, title, period) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("$code — $title", fontWeight = FontWeight.SemiBold)
                            Text("Période : $period", color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMiniCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(color),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, fontSize = MaterialTheme.typography.labelMedium.fontSize, color = Color.DarkGray)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardSectionCard(
    title: String,
    subtitle: String? = null,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(color),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(it, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}


@Composable
fun UserInfoCard(
    username: String,
    email: String,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Informations de l'utilisateur",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifier"
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Nom d'utilisateur : $username")
            Text("Email : $email")
        }
    }
}

@Composable
fun EditUserDialog(
    currentName: String,
    currentEmail: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var newEmail by remember { mutableStateOf(currentEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier mes informations") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nom d'utilisateur") }
                )
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("Email") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(newName, newEmail) }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
