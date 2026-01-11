package be.ecam.companion.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

data class AppSettingsController(
    val screenSizeMode: ScreenSizeMode,
    val textScaleMode: TextScaleMode,
    val themeMode: ThemeMode,
    val onZoomChange: () -> Unit,
    val onToggleTextScale: () -> Unit,
    val onToggleTheme: () -> Unit,
    val setScreenSizeMode: (ScreenSizeMode) -> Unit,
    val setTextScaleMode: (TextScaleMode) -> Unit,
    val setThemeMode: (ThemeMode) -> Unit
)

val LocalAppSettingsController = staticCompositionLocalOf {
    AppSettingsController(
        screenSizeMode = ScreenSizeMode.Default,
        textScaleMode = TextScaleMode.NORMAL,
        themeMode = ThemeMode.LIGHT,
        onZoomChange = {},
        onToggleTextScale = {},
        onToggleTheme = {},
        setScreenSizeMode = {},
        setTextScaleMode = {},
        setThemeMode = {}
    )
}
