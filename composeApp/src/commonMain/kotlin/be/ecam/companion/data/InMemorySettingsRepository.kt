package be.ecam.companion.data

import be.ecam.common.SERVER_PORT
import be.ecam.companion.ui.theme.ScreenSizeMode
import be.ecam.companion.ui.theme.TextScaleMode
import be.ecam.companion.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

class InMemorySettingsRepository(
    initialHost: String = defaultServerBaseUrl()
        .removePrefix("http://")
        .removePrefix("https://")
        .substringBefore(":"),
    initialPort: Int = SERVER_PORT,
    initialThemeMode: ThemeMode = ThemeMode.LIGHT,
    initialTextScaleMode: TextScaleMode = TextScaleMode.NORMAL,
    initialScreenSizeMode: ScreenSizeMode = ScreenSizeMode.Default,
    initialColorBlindMode: Boolean = false,
    initialFollowSystemSettings: Boolean = true,
) : SettingsRepository {
    private val host = MutableStateFlow(initialHost)
    private val port = MutableStateFlow(initialPort)
    private val themeMode = MutableStateFlow(initialThemeMode)
    private val textScaleMode = MutableStateFlow(initialTextScaleMode)
    private val screenSizeMode = MutableStateFlow(initialScreenSizeMode)
    private val colorBlindMode = MutableStateFlow(initialColorBlindMode)
    private val followSystemSettings = MutableStateFlow(initialFollowSystemSettings)

    override val serverHostFlow: Flow<String> = host
    override val serverPortFlow: Flow<Int> = port
    override val themeModeFlow: Flow<ThemeMode> = themeMode
    override val textScaleModeFlow: Flow<TextScaleMode> = textScaleMode
    override val screenSizeModeFlow: Flow<ScreenSizeMode> = screenSizeMode
    override val colorBlindModeFlow: Flow<Boolean> = colorBlindMode
    override val followSystemSettingsFlow: Flow<Boolean> = followSystemSettings

    override fun getServerHost(): String = host.value
    override fun getServerPort(): Int = port.value
    override fun getThemeMode(): ThemeMode = themeMode.value
    override fun getTextScaleMode(): TextScaleMode = textScaleMode.value
    override fun getScreenSizeMode(): ScreenSizeMode = screenSizeMode.value
    override fun getColorBlindMode(): Boolean = colorBlindMode.value
    override fun getFollowSystemSettings(): Boolean = followSystemSettings.value

    override fun setServerHost(host: String) {
        this.host.value = host
    }

    override fun setServerPort(port: Int) {
        this.port.value = port
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        this.themeMode.value = themeMode
    }

    override fun setTextScaleMode(textScaleMode: TextScaleMode) {
        this.textScaleMode.value = textScaleMode
    }

    override fun setScreenSizeMode(screenSizeMode: ScreenSizeMode) {
        this.screenSizeMode.value = screenSizeMode
    }

    override fun setColorBlindMode(enabled: Boolean) {
        this.colorBlindMode.value = enabled
    }

    override fun setFollowSystemSettings(enabled: Boolean) {
        this.followSystemSettings.value = enabled
    }
}
