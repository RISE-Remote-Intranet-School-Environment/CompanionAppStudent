package be.ecam.companion.data

import kotlinx.coroutines.flow.Flow
import be.ecam.companion.ui.theme.ScreenSizeMode
import be.ecam.companion.ui.theme.TextScaleMode
import be.ecam.companion.ui.theme.ThemeMode

interface SettingsRepository {
    val serverHostFlow: Flow<String>
    val serverPortFlow: Flow<Int>
    val themeModeFlow: Flow<ThemeMode>
    val textScaleModeFlow: Flow<TextScaleMode>
    val screenSizeModeFlow: Flow<ScreenSizeMode>
    val colorBlindModeFlow: Flow<Boolean>
    val followSystemSettingsFlow: Flow<Boolean>

    fun getServerHost(): String
    fun getServerPort(): Int
    fun getThemeMode(): ThemeMode
    fun getTextScaleMode(): TextScaleMode
    fun getScreenSizeMode(): ScreenSizeMode
    fun getColorBlindMode(): Boolean
    fun getFollowSystemSettings(): Boolean

    fun setServerHost(host: String)
    fun setServerPort(port: Int)
    fun setThemeMode(themeMode: ThemeMode)
    fun setTextScaleMode(textScaleMode: TextScaleMode)
    fun setScreenSizeMode(screenSizeMode: ScreenSizeMode)
    fun setColorBlindMode(enabled: Boolean)
    fun setFollowSystemSettings(enabled: Boolean)
}

