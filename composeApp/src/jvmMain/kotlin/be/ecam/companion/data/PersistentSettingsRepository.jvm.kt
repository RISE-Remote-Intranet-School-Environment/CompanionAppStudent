package be.ecam.companion.data

import be.ecam.common.SERVER_PORT
import be.ecam.companion.ui.theme.ScreenSizeMode
import be.ecam.companion.ui.theme.TextScaleMode
import be.ecam.companion.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.prefs.Preferences

class PersistentSettingsRepository : SettingsRepository {
    private val prefs: Preferences = Preferences.userRoot().node("be.ecam.companion")

    private val hostState = MutableStateFlow(loadHost())
    private val portState = MutableStateFlow(loadPort())
    private val themeModeState = MutableStateFlow(loadThemeMode())
    private val textScaleModeState = MutableStateFlow(loadTextScaleMode())
    private val screenSizeModeState = MutableStateFlow(loadScreenSizeMode())
    private val colorBlindModeState = MutableStateFlow(loadColorBlindMode())
    private val followSystemSettingsState = MutableStateFlow(loadFollowSystemSettings())

    override val serverHostFlow: Flow<String> = hostState
    override val serverPortFlow: Flow<Int> = portState
    override val themeModeFlow: Flow<ThemeMode> = themeModeState
    override val textScaleModeFlow: Flow<TextScaleMode> = textScaleModeState
    override val screenSizeModeFlow: Flow<ScreenSizeMode> = screenSizeModeState
    override val colorBlindModeFlow: Flow<Boolean> = colorBlindModeState
    override val followSystemSettingsFlow: Flow<Boolean> = followSystemSettingsState

    override fun getServerHost(): String = hostState.value
    override fun getServerPort(): Int = portState.value
    override fun getThemeMode(): ThemeMode = themeModeState.value
    override fun getTextScaleMode(): TextScaleMode = textScaleModeState.value
    override fun getScreenSizeMode(): ScreenSizeMode = screenSizeModeState.value
    override fun getColorBlindMode(): Boolean = colorBlindModeState.value
    override fun getFollowSystemSettings(): Boolean = followSystemSettingsState.value

    override fun setServerHost(host: String) {
        hostState.value = host
        prefs.put(KEY_HOST, host)
    }

    override fun setServerPort(port: Int) {
        portState.value = port
        prefs.putInt(KEY_PORT, port)
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        themeModeState.value = themeMode
        prefs.put(KEY_THEME, themeMode.name)
    }

    override fun setTextScaleMode(textScaleMode: TextScaleMode) {
        textScaleModeState.value = textScaleMode
        prefs.putFloat(KEY_TEXT_SCALE, textScaleMode.fontScale)
    }

    override fun setScreenSizeMode(screenSizeMode: ScreenSizeMode) {
        screenSizeModeState.value = screenSizeMode
        prefs.putFloat(KEY_SCREEN_SCALE, screenSizeMode.scale)
    }

    override fun setColorBlindMode(enabled: Boolean) {
        colorBlindModeState.value = enabled
        prefs.putBoolean(KEY_COLOR_BLIND, enabled)
    }

    override fun setFollowSystemSettings(enabled: Boolean) {
        followSystemSettingsState.value = enabled
        prefs.putBoolean(KEY_FOLLOW_SYSTEM, enabled)
    }

    private fun loadHost(): String {
        val def = defaultServerBaseUrl()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore(":")
        val stored = prefs.get(KEY_HOST, def)
        return if (stored.equals("localhost", ignoreCase = true) || stored == "127.0.0.1") {
            prefs.put(KEY_HOST, def)
            def
        } else {
            stored
        }
    }

    private fun loadPort(): Int {
        return prefs.getInt(KEY_PORT, SERVER_PORT)
    }

    private fun loadThemeMode(): ThemeMode {
        val stored = prefs.get(KEY_THEME, ThemeMode.LIGHT.name)
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.LIGHT)
    }

    private fun loadTextScaleMode(): TextScaleMode {
        val stored = prefs.getFloat(KEY_TEXT_SCALE, TextScaleMode.NORMAL.fontScale)
        return TextScaleMode.fromScale(stored)
    }

    private fun loadScreenSizeMode(): ScreenSizeMode {
        val stored = prefs.getFloat(KEY_SCREEN_SCALE, ScreenSizeMode.Default.scale)
        return ScreenSizeMode.fromScale(stored)
    }

    private fun loadColorBlindMode(): Boolean = prefs.getBoolean(KEY_COLOR_BLIND, false)

    private fun loadFollowSystemSettings(): Boolean = prefs.getBoolean(KEY_FOLLOW_SYSTEM, true)

    companion object {
        private const val KEY_HOST = "server_host"
        private const val KEY_PORT = "server_port"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_TEXT_SCALE = "text_scale"
        private const val KEY_SCREEN_SCALE = "screen_scale"
        private const val KEY_COLOR_BLIND = "color_blind"
        private const val KEY_FOLLOW_SYSTEM = "follow_system"
    }
}
