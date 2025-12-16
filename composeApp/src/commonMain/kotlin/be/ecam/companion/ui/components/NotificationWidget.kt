package be.ecam.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

data class NotificationItem(
    val title: String,
    val body: String
)

@Composable
fun NotificationWidget(
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    // 6 notifications pour tester
    val notifications = listOf(
        NotificationItem("Réunion d'évaluation","Évaluation du cours d'Électronique Analogique prévue le 12/12/2025."
        ),
        NotificationItem("Correction d'examens","Les copies de la session de janvier sont disponibles au secrétariat."
        ),
        NotificationItem("Conseil de classe","Le conseil de classe du bloc 2 est fixé au 18/12 à 14h."
        ),
        NotificationItem("Horaire mis à jour", "Votre cours de Laboratoire a été déplacé au local E217 ce vendredi."
        ),
        NotificationItem("Demande d'étudiant","Un étudiant a demandé un rendez-vous pour revoir le chapitre 4."
        ),
        NotificationItem("Publication des notes","La date limite pour encoder les notes est fixée au 20/12."
        )
    )

    // On limite à 5 (les plus récentes)
    val latestNotifications = notifications.takeLast(5).reversed()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(340.dp)
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.padding(8.dp)
        ) {
            Column(Modifier.padding(16.dp)) {

                // Header
                Text(
                    "Notifications",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(12.dp))

                // Liste sans LazyColumn
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    latestNotifications.forEach { notif ->
                        NotificationCardItem(notif)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCardItem(n: NotificationItem) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                n.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                n.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            )
        }
    }
}
