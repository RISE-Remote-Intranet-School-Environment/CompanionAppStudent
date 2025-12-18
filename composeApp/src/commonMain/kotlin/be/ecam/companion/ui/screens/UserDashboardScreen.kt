package be.ecam.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import be.ecam.companion.viewmodel.LoginViewModel
import be.ecam.companion.ui.components.RemoteImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

@Composable
fun UserDashboardScreen(loginViewModel: LoginViewModel, modifier: Modifier = Modifier) {
    val user = loginViewModel.currentUser ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune donnée utilisateur.")
        }
        return
    }

    val scrollState = rememberScrollState()

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

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // HEADER
            Row(verticalAlignment = Alignment.CenterVertically) {
                val avatarUrl = loginViewModel.currentUser?.avatarUrl
                if (!avatarUrl.isNullOrBlank()) {
                    RemoteImage(
                        url = avatarUrl,
                        contentDescription = "Photo de profil",
                        modifier = Modifier.size(70.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        "Bonjour, ${user.username}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Heureux de vous revoir 👋",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // QUICK STATS
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardMiniCard(
                    title = "ECTS",
                    value = "60",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                DashboardMiniCard(
                    title = "Bloc",
                    value = "4",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                DashboardMiniCard(
                    title = "Cours",
                    value = paeCourses.size.toString(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            UserInfoCard(
                username = user.username,
                email = user.email,
                onEditClick = { showEditDialog = true }
            )

            DashboardSectionCard(
                title = "Derniers messages",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                onContainerColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                lastMessages.forEach {
                    Text("• $it", Modifier.padding(vertical = 4.dp))
                }
            }

            DashboardSectionCard(
                title = "Votre PAE",
                subtitle = "Année académique 2025-2026 — Bloc 4 — Programme 4MIN",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                paeCourses.forEach { (code, title, period) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text("$code — $title", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Période : $period",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMiniCard(
        title: String,
        value: String,
        containerColor: Color,
        contentColor: Color,
        modifier: Modifier = Modifier
) {
    Card(
            modifier = modifier.height(90.dp),
            shape = RoundedCornerShape(20.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor = containerColor,
                            contentColor = contentColor
                    ),
            elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DashboardSectionCard(
    title: String,
    subtitle: String? = null,
    containerColor: Color,
    onContainerColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = onContainerColor
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainerColor.copy(alpha = 0.75f)
                )
            }

            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}


@Composable
fun UserInfoCard(username: String, email: String, onEditClick: () -> Unit) {
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
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier")
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
                Button(onClick = { onSave(newName, newEmail) }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
