package be.ecam.companion.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import be.ecam.companion.ui.components.NotificationWidget
import be.ecam.companion.ui.theme.ScreenSizeMode
import be.ecam.companion.ui.theme.TextScaleMode
import be.ecam.companion.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    selectedScreen: BottomItem,
    showCoursesPage: Boolean,
    showProfessorsPage: Boolean,
    showPaePage: Boolean,
    coursesTitleSuffix: String?,
    paeTitleSuffix: String?,
    screenSizeMode: ScreenSizeMode,
    onZoomChange: () -> Unit,
    textScaleMode: TextScaleMode,
    onToggleTextScale: () -> Unit,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
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

            IconButton(onClick = onZoomChange) {
                Icon(
                    Icons.Filled.ZoomIn,
                    contentDescription = screenSizeMode.description()
                )
            }

            IconButton(onClick = onToggleTextScale) {
                Icon(
                    Icons.Filled.FormatSize,
                    contentDescription = textScaleMode.description()
                )
            }

            IconToggleButton(
                checked = themeMode.isDark,
                onCheckedChange = { onToggleTheme() }
            ) {
                Icon(
                    if (themeMode.isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = if (themeMode.isDark) "Passer en mode clair" else "Passer en mode sombre"
                )
            }

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
