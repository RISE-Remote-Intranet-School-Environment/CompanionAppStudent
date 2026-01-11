package be.ecam.companion.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    LIGHT,
    DARK;

    val isDark: Boolean
        get() = this == DARK

    fun toggle(): ThemeMode = if (this == DARK) LIGHT else DARK

    fun colorScheme(colorBlind: Boolean = false): ColorScheme = when (this) {
        LIGHT -> if (colorBlind) colorBlindLightScheme else lightColorScheme()
        DARK -> if (colorBlind) colorBlindDarkScheme else darkColorScheme()
    }
}

private val colorBlindLightScheme = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFB3261E),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF006874),
    onTertiary = Color(0xFFFFFFFF)
)

private val colorBlindDarkScheme = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color(0xFF001C3B),
    secondary = Color(0xFFFFB4A9),
    onSecondary = Color(0xFF690005),
    tertiary = Color(0xFF4FD8EB),
    onTertiary = Color(0xFF00363E)
)
