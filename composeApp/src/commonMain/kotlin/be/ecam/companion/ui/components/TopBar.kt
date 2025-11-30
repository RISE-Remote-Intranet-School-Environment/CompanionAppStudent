package be.ecam.companion.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    onMenuClick: () -> Unit
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
        }
    )
}
