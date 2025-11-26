package be.ecam.companion.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable

enum class BottomItem {
    HOME,
    COURSECALENDAR,
    EVENTCALENDAR,
    SETTINGS,
    DASHBOARD;

    @Composable
    fun getLabel(): String =
        when (this) {
            HOME -> "Accueil"
            COURSECALENDAR -> "Cours"
            EVENTCALENDAR -> "Événements"
            SETTINGS -> "Paramètres"
            DASHBOARD -> "Dashboard"
        }

    fun getIconRes() =
        when (this) {
            HOME -> Icons.Filled.Home
            EVENTCALENDAR -> Icons.Filled.CalendarMonth
            COURSECALENDAR -> Icons.Filled.CalendarMonth
            SETTINGS -> Icons.Filled.Settings
            DASHBOARD -> Icons.Filled.Dashboard
        }

    companion object {
        val entries = values()
    }
}
