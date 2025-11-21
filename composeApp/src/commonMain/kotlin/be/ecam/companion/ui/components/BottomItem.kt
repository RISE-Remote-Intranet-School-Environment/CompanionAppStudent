package be.ecam.companion.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable

enum class BottomItem {
    HOME,
    CALENDAR,
    SETTINGS,
    DASHBOARD;

    @Composable
    fun getLabel(): String =
        when (this) {
            HOME -> "Accueil"
            CALENDAR -> "Calendrier"
            SETTINGS -> "Paramètres"
            DASHBOARD -> "Dashboard"
        }

    fun getIconRes() =
        when (this) {
            HOME -> Icons.Filled.Home
            CALENDAR -> Icons.Filled.CalendarMonth
            SETTINGS -> Icons.Filled.Settings
            DASHBOARD -> Icons.Filled.Dashboard
        }

    companion object {
        val entries = values()
    }
}
