package be.ecam.companion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@Composable
internal actual fun systemPrefersDarkTheme(): Boolean? = isSystemInDarkTheme()

@Composable
internal actual fun systemFontScale(): Float? = LocalDensity.current.fontScale
