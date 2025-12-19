package be.ecam.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import be.ecam.companion.data.CalendarRepository
import be.ecam.companion.data.PaeCourse
import be.ecam.companion.data.PaeRecord
import be.ecam.companion.data.PaeRepository
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.di.buildBaseUrl
import be.ecam.companion.ui.components.RemoteImage
import be.ecam.companion.viewmodel.AuthUserDTO
import be.ecam.companion.viewmodel.LoginViewModel
import io.ktor.client.HttpClient
import org.koin.compose.koinInject

private data class DashboardData(
    val user: AuthUserDTO,
    val studentDisplayName: String,
    val record: PaeRecord?,
    val courses: List<PaeCourse>,
    val events: List<CalendarEvent>
)

@Composable
fun UserDashboardScreen(loginViewModel: LoginViewModel, modifier: Modifier = Modifier) {
    val httpClient = koinInject<HttpClient>()
    val settingsRepo = koinInject<SettingsRepository>()
    val host = settingsRepo.getServerHost()
    val port = settingsRepo.getServerPort()
    val token = loginViewModel.jwtToken?.trim()?.removeSurrounding("\"")
    val user = loginViewModel.currentUser
    val scrollState = rememberScrollState()

    var showEditDialog by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val dashboardData by produceState<DashboardData?>(
        initialValue = null,
        host,
        port,
        user,
        token
    ) {
        loadError = null
        if (user == null) {
            value = null
            return@produceState
        }

        val baseUrl = buildBaseUrl(host, port)
        value = try {
            val paeDatabase = runCatching {
                loadPaeFromServer(
                    client = httpClient,
                    baseUrl = baseUrl,
                    token = token
                )
            }.getOrElse { null } ?: PaeRepository.load()

            val targetStudent = paeDatabase.students.firstOrNull {
                it.email.equals(user.email, ignoreCase = true) ||
                        it.username.equals(user.username, ignoreCase = true)
            } ?: paeDatabase.students.firstOrNull()

            val record = targetStudent?.records
                ?.sortedByDescending { it.catalogYear ?: it.academicYearLabel ?: "" }
                ?.firstOrNull()

            val events = runCatching {
                CalendarRepository(
                    client = httpClient,
                    baseUrlProvider = { baseUrl }
                ).getCalendarEvents(token)
            }.getOrDefault(emptyList())

            DashboardData(
                user = user,
                studentDisplayName = targetStudent?.username ?: user.username,
                record = record,
                courses = record?.courses ?: emptyList(),
                events = events
            )
        } catch (e: Exception) {
            loadError = e.message
            null
        }
    }

    when {
        user == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun utilisateur connecté.")
            }
            return
        }

        loadError != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(loadError ?: "Erreur inconnue", color = MaterialTheme.colorScheme.error)
            }
            return
        }

        dashboardData == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
    }

    val data = dashboardData!!
    val totalEcts = data.record?.courses?.sumOf { it.ects ?: 0 } ?: 0
    val blocValue = data.record?.block ?: "-"
    val courseCount = data.courses.size
    val paeSubtitle = buildString {
        append("Année académique ")
        append(data.record?.catalogYear ?: data.record?.academicYearLabel ?: "-")
        append(" • ")
        append(blocValue)
        append(" • Programme ")
        append(data.record?.program ?: data.record?.formationSlug ?: "-")
    }
    val lastMessages = data.events
        .sortedBy { it.date }
        .take(5)
        .map { event -> "${event.title} • ${event.date}" }

    if (showEditDialog) {
        EditUserDialog(
            currentName = data.user.username,
            currentEmail = data.user.email,
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
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column {
                    val displayName = listOfNotNull(user.firstName, user.lastName)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { user.username }
                    Text(
                        "Bonjour, $displayName",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Heureux de vous revoir !",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // QUICK STATS
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardMiniCard(
                    title = "ECTS",
                    value = totalEcts.toString(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                DashboardMiniCard(
                    title = "Bloc",
                    value = blocValue,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                DashboardMiniCard(
                    title = "Cours",
                    value = courseCount.toString(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            UserInfoCard(
                username = data.user.username,
                email = data.user.email,
                onEditClick = { showEditDialog = true }
            )

            DashboardSectionCard(
                title = "Derniers messages",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                onContainerColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                if (lastMessages.isEmpty()) {
                    Text("Aucune notification récente.", Modifier.padding(vertical = 4.dp))
                } else {
                    lastMessages.forEach {
                        Text("• $it", Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            DashboardSectionCard(
                title = "Votre PAE",
                subtitle = paeSubtitle,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                if (data.courses.isEmpty()) {
                    Text("Aucun cours trouvé pour votre PAE.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                } else {
                    data.courses.forEach { course ->
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
                            Text(
                                "${course.code?.uppercase() ?: "?"} • ${course.title ?: "Titre inconnu"}",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Période : ${course.period ?: "-"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
