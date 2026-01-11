package be.ecam.companion.ui.theme

import androidx.compose.runtime.Composable

@Composable
internal expect fun systemPrefersDarkTheme(): Boolean?

@Composable
internal expect fun systemFontScale(): Float?
