package be.ecam.companion.data

import be.ecam.common.SERVER_PORT
import be.ecam.companion.ui.theme.ScreenSizeMode
import be.ecam.companion.ui.theme.TextScaleMode
import be.ecam.companion.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSUserDefaults

class PersistentSettingsRepository : SettingsRepository {
    private val defaults = NSUserDefaults.standardUserDefaults()

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
        defaults.setObject(host, forKey = KEY_HOST)
    }

    override fun setServerPort(port: Int) {
        portState.value = port
        defaults.setInteger(port.toLong(), forKey = KEY_PORT)
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        themeModeState.value = themeMode
        defaults.setObject(themeMode.name, forKey = KEY_THEME)
    }

    override fun setTextScaleMode(textScaleMode: TextScaleMode) {
        textScaleModeState.value = textScaleMode
        defaults.setDouble(textScaleMode.fontScale.toDouble(), forKey = KEY_TEXT_SCALE)
    }

    override fun setScreenSizeMode(screenSizeMode: ScreenSizeMode) {
        screenSizeModeState.value = screenSizeMode
        defaults.setDouble(screenSizeMode.scale.toDouble(), forKey = KEY_SCREEN_SCALE)
    }

    override fun setColorBlindMode(enabled: Boolean) {
        colorBlindModeState.value = enabled
        defaults.setBool(enabled, forKey = KEY_COLOR_BLIND)
    }

    override fun setFollowSystemSettings(enabled: Boolean) {
        followSystemSettingsState.value = enabled
        defaults.setBool(enabled, forKey = KEY_FOLLOW_SYSTEM)
    }

    private fun loadHost(): String {
        val def = defaultServerBaseUrl().removePrefix("http://").removePrefix("https://").substringBefore(":")
        val value = defaults.stringForKey(KEY_HOST)
        return value ?: def
    }

    private fun loadPort(): Int {
        val stored = defaults.objectForKey(KEY_PORT)
        return when (stored) {
            is Long -> stored.toInt()
            is Int -> stored
            else -> SERVER_PORT
        }
    }

    private fun loadThemeMode(): ThemeMode {
        val stored = defaults.stringForKey(KEY_THEME) ?: ThemeMode.LIGHT.name
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.LIGHT)
    }

    private fun loadTextScaleMode(): TextScaleMode {
        val stored = defaults.doubleForKey(KEY_TEXT_SCALE)
        return if (stored > 0.0) TextScaleMode.fromScale(stored.toFloat()) else TextScaleMode.NORMAL
    }

    private fun loadScreenSizeMode(): ScreenSizeMode {
        val stored = defaults.doubleForKey(KEY_SCREEN_SCALE)
        return if (stored > 0.0) ScreenSizeMode.fromScale(stored.toFloat()) else ScreenSizeMode.Default
    }

    private fun loadColorBlindMode(): Boolean = defaults.boolForKey(KEY_COLOR_BLIND)

    private fun loadFollowSystemSettings(): Boolean {
        return if (defaults.objectForKey(KEY_FOLLOW_SYSTEM) == null) {
            true
        } else {
            defaults.boolForKey(KEY_FOLLOW_SYSTEM)
        }
    }

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
