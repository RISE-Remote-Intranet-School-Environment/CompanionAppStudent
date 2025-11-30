package be.ecam.companion.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

enum class ThemeMode {
    LIGHT,
    DARK;

    val isDark: Boolean
        get() = this == DARK

    fun toggle(): ThemeMode = if (this == DARK) LIGHT else DARK

    fun colorScheme(): ColorScheme = when (this) {
        LIGHT -> lightColorScheme()
        DARK -> darkColorScheme()
    }
}
