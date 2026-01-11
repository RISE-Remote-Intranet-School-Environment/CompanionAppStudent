package be.ecam.companion.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import be.ecam.companion.ui.components.NotificationWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    selectedScreen: BottomItem,
    showCoursesPage: Boolean,
    showProfessorsPage: Boolean,
    showPaePage: Boolean,
    coursesTitleSuffix: String?,
    paeTitleSuffix: String?,
    onMenuClick: () -> Unit,
    showNotifications: Boolean,
    onNotificationsClick: () -> Unit
) {
    TopAppBar(
        
        title = {
            when {
                showCoursesPage ->
                    Text(coursesTitleSuffix?.let { "Formations - $it" } ?: "Formations")

                showProfessorsPage ->
                    Text("Professeurs")

                showPaePage ->
                    Text(paeTitleSuffix ?: "Mon PAE")

                else ->
                    Text(selectedScreen.getLabel())
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "Open drawer")
            }
        },
        actions = {
            IconButton(onClick = { /* message mode */ }) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message mode")
            }

            // ---- Notifications icon ----
            // --- Notifications icon + menu (anchor dans le même scope) ---
            Box {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.size(48.dp) // garde une taille d'anchor raisonnable
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }

                // NotificationWidget est rendu ici : même scope que l'icône (anchor)
                NotificationWidget(
                    expanded = showNotifications,
                    onDismiss = { onNotificationsClick() } // inverse l'état côté appelant
                )
            }


        }
    )
}
