package be.ecam.companion.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import be.ecam.companion.ui.components.BottomItem
import androidx.compose.foundation.layout.Spacer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    selectedScreen: BottomItem,
    showCoursesPage: Boolean,
    coursesTitleSuffix: String?,
    onMenuClick: () -> Unit
) {
    TopAppBar(
        title = {
            if (showCoursesPage) {
                val dynamicTitle = coursesTitleSuffix?.let { "Formations - $it" } ?: "Formations"
                Text(dynamicTitle)
            } else {
                Text(selectedScreen.getLabel())
            }
        },
        navigationIcon = {
            // Pas d’icône dans le calendrier
            if (!showCoursesPage && selectedScreen == BottomItem.CALENDAR) {
                Spacer(Modifier)
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, contentDescription = "Open drawer")
                }
            }
        }
    )
}
