package be.ecam.companion.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.sun.jna.platform.win32.Advapi32
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.platform.win32.WinReg.HKEYByReference
import java.awt.Toolkit
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal actual fun systemPrefersDarkTheme(): Boolean? {
    val themeState = remember { mutableStateOf(detectSystemDarkTheme()) }

    DisposableEffect(Unit) {
        val watcher = createThemeWatcher { newValue ->
            if (themeState.value != newValue) {
                themeState.value = newValue
            }
        }
        watcher?.start()
        onDispose { watcher?.stop() }
    }

    return themeState.value
}

@Composable
internal actual fun systemFontScale(): Float? {
    val scaleState = remember { mutableStateOf(detectSystemFontScale()) }

    DisposableEffect(Unit) {
        val watcher = createFontScaleWatcher { newValue ->
            if (scaleState.value != newValue) {
                scaleState.value = newValue
            }
        }
        watcher?.start()
        onDispose { watcher?.stop() }
    }

    return scaleState.value
}

private fun detectSystemDarkTheme(): Boolean? {
    val osName = System.getProperty("os.name")?.lowercase() ?: return null
    return when {
        osName.contains("win") -> detectWindowsDarkTheme()
        osName.contains("mac") -> detectMacDarkTheme()
        osName.contains("linux") -> detectLinuxDarkTheme()
        else -> null
    }
}

private fun detectSystemFontScale(): Float? {
    val osName = System.getProperty("os.name")?.lowercase() ?: return null
    return when {
        osName.contains("win") -> detectWindowsFontScale()
        osName.contains("mac") -> detectMacFontScale()
        osName.contains("linux") -> detectLinuxFontScale()
        else -> detectToolkitScale()
    }
}

private fun createThemeWatcher(onChange: (Boolean?) -> Unit): ThemeWatcher? {
    val osName = System.getProperty("os.name")?.lowercase() ?: return null
    return when {
        osName.contains("win") -> WindowsThemeWatcher(onChange)
        osName.contains("mac") || osName.contains("darwin") -> MacThemeWatcher(onChange)
        osName.contains("linux") -> LinuxThemeWatcher(onChange)
        else -> null
    }
}

private fun createFontScaleWatcher(onChange: (Float?) -> Unit): ScaleWatcher? {
    val osName = System.getProperty("os.name")?.lowercase() ?: return null
    return when {
        osName.contains("win") -> WindowsFontScaleWatcher(onChange)
        osName.contains("mac") || osName.contains("darwin") -> MacFontScaleWatcher(onChange)
        osName.contains("linux") -> createLinuxFontScaleWatcher(onChange)
        else -> null
    }
}

private fun createLinuxFontScaleWatcher(onChange: (Float?) -> Unit): ScaleWatcher? {
    val target = detectLinuxFontScaleReading()?.watchTarget ?: return null
    return object : FileScaleWatcher(target.directory, target.fileName, onChange) {
        override fun resolveScale(): Float? = detectLinuxFontScale()
    }
}

private fun detectWindowsDarkTheme(): Boolean? {
    return runCatching {
        val value = Advapi32Util.registryGetIntValue(
            WinReg.HKEY_CURRENT_USER,
            WINDOWS_THEME_KEY,
            WINDOWS_THEME_VALUE
        )
        value == 0
    }.getOrNull()
}

private fun detectMacDarkTheme(): Boolean? {
    val output = runCommand(listOf("defaults", "read", "-g", "AppleInterfaceStyle"))
    return output?.contains("Dark", ignoreCase = true) ?: false
}

private fun detectLinuxDarkTheme(): Boolean? {
    val output = runCommand(listOf("gsettings", "get", "org.gnome.desktop.interface", "color-scheme"))
    if (output != null) {
        if (output.contains("dark", ignoreCase = true)) return true
        if (output.contains("light", ignoreCase = true)) return false
    }
    val gtkTheme = System.getenv("GTK_THEME") ?: return null
    return gtkTheme.contains("dark", ignoreCase = true)
}

private fun detectWindowsFontScale(): Float? {
    val textScale = runCatching {
        Advapi32Util.registryGetIntValue(
            WinReg.HKEY_CURRENT_USER,
            WINDOWS_TEXT_SIZE_KEY,
            WINDOWS_TEXT_SIZE_VALUE
        )
    }.getOrNull()
    if (textScale != null && textScale > 0) {
        return textScale / 100f
    }
    val dpi = runCatching {
        Advapi32Util.registryGetIntValue(
            WinReg.HKEY_CURRENT_USER,
            WINDOWS_DPI_KEY,
            WINDOWS_DPI_VALUE
        )
    }.getOrNull()
    return if (dpi != null && dpi > 0) dpi / 96f else detectToolkitScale()
}

private fun detectMacFontScale(): Float? {
    val hostScale = readDefaultsFloat(listOf("defaults", "-currentHost", "read", "-g", "AppleDisplayScaleFactor"))
    if (hostScale != null && hostScale > 0f) return hostScale
    val globalScale = readDefaultsFloat(listOf("defaults", "read", "-g", "AppleDisplayScaleFactor"))
    return if (globalScale != null && globalScale > 0f) globalScale else detectToolkitScale()
}

private fun detectLinuxFontScale(): Float? {
    return detectLinuxFontScaleReading()?.value ?: detectToolkitScale()
}

private data class WatchTarget(val directory: Path, val fileName: String)
private data class ScaleReading(val value: Float, val watchTarget: WatchTarget?)

private fun detectLinuxFontScaleReading(): ScaleReading? {
    val gnomeTextScale = readGSettingsFloat("org.gnome.desktop.interface", "text-scaling-factor")
    if (gnomeTextScale != null && gnomeTextScale > 0f) {
        return ScaleReading(gnomeTextScale, linuxDconfWatchTarget())
    }

    val gnomeScale = readGSettingsFloat("org.gnome.desktop.interface", "scaling-factor")
    if (gnomeScale != null && gnomeScale > 0f) {
        return ScaleReading(gnomeScale, linuxDconfWatchTarget())
    }

    readKdeFontScale()?.let { return it }
    readXresourcesFontScale()?.let { return it }

    val envScale = readLinuxEnvScale()
    if (envScale != null && envScale > 0f) {
        return ScaleReading(envScale, null)
    }

    return null
}

private fun readDefaultsFloat(command: List<String>): Float? = parseNumericValue(runCommand(command))

private fun readGSettingsFloat(schema: String, key: String): Float? {
    return parseNumericValue(runCommand(listOf("gsettings", "get", schema, key)))
}

private fun linuxDconfWatchTarget(): WatchTarget? {
    val home = System.getProperty("user.home") ?: return null
    val dir = Paths.get(home, ".config", "dconf")
    return if (dir.toFile().exists()) WatchTarget(dir, "user") else null
}

private fun readKdeFontScale(): ScaleReading? {
    val home = System.getProperty("user.home") ?: return null
    val kcmFonts = Paths.get(home, ".config", "kcmfonts")
    readKdeFontScaleFromFile(kcmFonts)?.let { scale ->
        return ScaleReading(scale, watchTargetForPath(kcmFonts))
    }
    val kdeGlobals = Paths.get(home, ".config", "kdeglobals")
    readKdeFontScaleFromFile(kdeGlobals)?.let { scale ->
        return ScaleReading(scale, watchTargetForPath(kdeGlobals))
    }
    return null
}

private fun readKdeFontScaleFromFile(path: Path): Float? {
    val content = readFileText(path) ?: return null
    val match = Regex("(?im)^\\s*(forceFontDPI|ForceFontDPI|XftDPI)\\s*=\\s*([^\\s#;]+)").find(content)
    val dpi = parseNumericValue(match?.groupValues?.get(2))
    return dpi?.takeIf { it > 0f }?.div(96f)
}

private fun readXresourcesFontScale(): ScaleReading? {
    val home = System.getProperty("user.home") ?: return null
    val xresources = Paths.get(home, ".Xresources")
    readXresourcesFontScaleFromFile(xresources)?.let { scale ->
        return ScaleReading(scale, watchTargetForPath(xresources))
    }
    val xdefaults = Paths.get(home, ".Xdefaults")
    readXresourcesFontScaleFromFile(xdefaults)?.let { scale ->
        return ScaleReading(scale, watchTargetForPath(xdefaults))
    }
    return null
}

private fun readXresourcesFontScaleFromFile(path: Path): Float? {
    val content = readFileText(path) ?: return null
    val match = Regex("(?im)^\\s*Xft\\.dpi\\s*[:=]\\s*([^\\s#;]+)").find(content)
    val dpi = parseNumericValue(match?.groupValues?.get(1))
    return dpi?.takeIf { it > 0f }?.div(96f)
}

private fun readLinuxEnvScale(): Float? {
    val qtScale = parseNumericValue(System.getenv("QT_SCALE_FACTOR"))
    if (qtScale != null && qtScale > 0f) return qtScale

    val gdkScale = parseNumericValue(System.getenv("GDK_SCALE"))
    val gdkDpiScale = parseNumericValue(System.getenv("GDK_DPI_SCALE"))
    if (gdkScale != null || gdkDpiScale != null) {
        val scale = (gdkScale ?: 1f) * (gdkDpiScale ?: 1f)
        return scale.takeIf { it > 0f }
    }

    val qtFontDpi = parseNumericValue(System.getenv("QT_FONT_DPI"))
    if (qtFontDpi != null && qtFontDpi > 0f) return qtFontDpi / 96f

    val xftDpi = parseNumericValue(System.getenv("XFT_DPI"))
    if (xftDpi != null && xftDpi > 0f) return xftDpi / 96f

    return null
}

private fun watchTargetForPath(path: Path): WatchTarget? {
    val parent = path.parent ?: return null
    return WatchTarget(parent, path.fileName.toString())
}

private fun readFileText(path: Path): String? {
    return runCatching { path.toFile().readText() }.getOrNull()
}

private fun parseNumericValue(value: String?): Float? {
    if (value == null) return null
    val matches = Regex("(?<![A-Za-z])[-+]?\\d*\\.?\\d+(?![A-Za-z])")
        .findAll(value.trim())
    val last = matches.lastOrNull()?.value ?: return null
    return last.toFloatOrNull()
}

private fun detectToolkitScale(): Float? {
    return runCatching {
        val dpi = Toolkit.getDefaultToolkit().screenResolution
        if (dpi > 0) dpi / 96f else null
    }.getOrNull()
}

private fun runCommand(command: List<String>): String? {
    return try {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(1, TimeUnit.SECONDS)) {
            process.destroy()
            return null
        }
        val output = process.inputStream.bufferedReader().readText().trim()
        output.ifBlank { null }
    } catch (_: Exception) {
        null
    }
}

private interface ThemeWatcher {
    fun start()
    fun stop()
}

private interface ScaleWatcher {
    fun start()
    fun stop()
}

private abstract class FileThemeWatcher(
    private val watchDir: Path,
    private val fileName: String,
    private val onChange: (Boolean?) -> Unit
) : ThemeWatcher {
    private val running = AtomicBoolean(true)
    private var watchService = FileSystems.getDefault().newWatchService()
    private val thread = Thread { watchLoop() }.apply {
        isDaemon = true
        name = "SystemThemeWatcher"
    }

    override fun start() {
        if (!watchDir.toFile().exists()) {
            onChange(null)
            try {
                watchService.close()
            } catch (_: Exception) {
                // no-op
            }
            return
        }
        try {
            watchDir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE
            )
        } catch (_: Exception) {
            onChange(null)
            try {
                watchService.close()
            } catch (_: Exception) {
                // no-op
            }
            return
        }
        thread.start()
    }

    override fun stop() {
        running.set(false)
        try {
            watchService.close()
        } catch (_: Exception) {
            // no-op
        }
    }

    protected abstract fun resolveTheme(): Boolean?

    private fun watchLoop() {
        onChange(resolveTheme())
        try {
            while (running.get()) {
                val key = watchService.take()
                key.pollEvents().forEach { event ->
                    val changed = event.context().toString()
                    if (changed == fileName) {
                        onChange(resolveTheme())
                    }
                }
                if (!key.reset()) {
                    break
                }
            }
        } catch (_: ClosedWatchServiceException) {
            // watcher closed
        } catch (_: Exception) {
            onChange(null)
        }
    }
}

private abstract class FileScaleWatcher(
    private val watchDir: Path,
    private val fileName: String,
    private val onChange: (Float?) -> Unit
) : ScaleWatcher {
    private val running = AtomicBoolean(true)
    private var watchService = FileSystems.getDefault().newWatchService()
    private val thread = Thread { watchLoop() }.apply {
        isDaemon = true
        name = "SystemFontScaleWatcher"
    }

    override fun start() {
        if (!watchDir.toFile().exists()) {
            onChange(null)
            try {
                watchService.close()
            } catch (_: Exception) {
                // no-op
            }
            return
        }
        try {
            watchDir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE
            )
        } catch (_: Exception) {
            onChange(null)
            try {
                watchService.close()
            } catch (_: Exception) {
                // no-op
            }
            return
        }
        thread.start()
    }

    override fun stop() {
        running.set(false)
        try {
            watchService.close()
        } catch (_: Exception) {
            // no-op
        }
    }

    protected abstract fun resolveScale(): Float?

    private fun watchLoop() {
        onChange(resolveScale())
        try {
            while (running.get()) {
                val key = watchService.take()
                key.pollEvents().forEach { event ->
                    val changed = event.context().toString()
                    if (changed == fileName) {
                        onChange(resolveScale())
                    }
                }
                if (!key.reset()) {
                    break
                }
            }
        } catch (_: ClosedWatchServiceException) {
            // watcher closed
        } catch (_: Exception) {
            onChange(null)
        }
    }
}

private class MacThemeWatcher(
    onChange: (Boolean?) -> Unit
) : FileThemeWatcher(
    watchDir = Paths.get(System.getProperty("user.home"), "Library", "Preferences"),
    fileName = ".GlobalPreferences.plist",
    onChange = onChange
) {
    override fun resolveTheme(): Boolean? = detectMacDarkTheme()
}

private class LinuxThemeWatcher(
    onChange: (Boolean?) -> Unit
) : FileThemeWatcher(
    watchDir = Paths.get(System.getProperty("user.home"), ".config", "dconf"),
    fileName = "user",
    onChange = onChange
) {
    override fun resolveTheme(): Boolean? = detectLinuxDarkTheme()
}

private class MacFontScaleWatcher(
    onChange: (Float?) -> Unit
) : FileScaleWatcher(
    watchDir = Paths.get(System.getProperty("user.home"), "Library", "Preferences"),
    fileName = ".GlobalPreferences.plist",
    onChange = onChange
) {
    override fun resolveScale(): Float? = detectMacFontScale()
}

private class WindowsThemeWatcher(
    private val onChange: (Boolean?) -> Unit
) : ThemeWatcher {
    private val running = AtomicBoolean(true)
    private val thread = Thread { watchLoop() }.apply {
        isDaemon = true
        name = "SystemThemeWatcher"
    }

    override fun start() {
        thread.start()
    }

    override fun stop() {
        running.set(false)
    }

    private fun watchLoop() {
        val keyRef = HKEYByReference()
        val openResult = Advapi32.INSTANCE.RegOpenKeyEx(
            WinReg.HKEY_CURRENT_USER,
            WINDOWS_THEME_KEY,
            0,
            WinNT.KEY_READ,
            keyRef
        )
        if (openResult != WinError.ERROR_SUCCESS) {
            onChange(null)
            return
        }

        val key = keyRef.value
        val eventHandle = Kernel32.INSTANCE.CreateEvent(null, true, false, null)
        if (eventHandle == null) {
            Advapi32.INSTANCE.RegCloseKey(key)
            return
        }

        try {
            onChange(detectWindowsDarkTheme())
            while (running.get()) {
                val notifyResult = Advapi32.INSTANCE.RegNotifyChangeKeyValue(
                    key,
                    false,
                    WinNT.REG_NOTIFY_CHANGE_LAST_SET,
                    eventHandle,
                    true
                )
                if (notifyResult != WinError.ERROR_SUCCESS) {
                    break
                }

                while (running.get()) {
                    when (Kernel32.INSTANCE.WaitForSingleObject(eventHandle, 1000)) {
                        WinBase.WAIT_OBJECT_0 -> {
                            Kernel32.INSTANCE.ResetEvent(eventHandle)
                            onChange(detectWindowsDarkTheme())
                            break
                        }
                        WAIT_TIMEOUT -> continue
                        else -> return
                    }
                }
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(eventHandle)
            Advapi32.INSTANCE.RegCloseKey(key)
        }
    }
}

private class WindowsFontScaleWatcher(
    private val onChange: (Float?) -> Unit
) : ScaleWatcher {
    private val running = AtomicBoolean(true)
    private val thread = Thread { watchLoop() }.apply {
        isDaemon = true
        name = "SystemFontScaleWatcher"
    }

    override fun start() {
        thread.start()
    }

    override fun stop() {
        running.set(false)
    }

    private fun watchLoop() {
        val keyRef = HKEYByReference()
        val openResult = Advapi32.INSTANCE.RegOpenKeyEx(
            WinReg.HKEY_CURRENT_USER,
            WINDOWS_TEXT_SIZE_KEY,
            0,
            WinNT.KEY_READ,
            keyRef
        )
        if (openResult != WinError.ERROR_SUCCESS) {
            onChange(null)
            return
        }

        val key = keyRef.value
        val eventHandle = Kernel32.INSTANCE.CreateEvent(null, true, false, null)
        if (eventHandle == null) {
            Advapi32.INSTANCE.RegCloseKey(key)
            return
        }

        try {
            onChange(detectWindowsFontScale())
            while (running.get()) {
                val notifyResult = Advapi32.INSTANCE.RegNotifyChangeKeyValue(
                    key,
                    false,
                    WinNT.REG_NOTIFY_CHANGE_LAST_SET,
                    eventHandle,
                    true
                )
                if (notifyResult != WinError.ERROR_SUCCESS) {
                    break
                }

                while (running.get()) {
                    when (Kernel32.INSTANCE.WaitForSingleObject(eventHandle, 1000)) {
                        WinBase.WAIT_OBJECT_0 -> {
                            Kernel32.INSTANCE.ResetEvent(eventHandle)
                            onChange(detectWindowsFontScale())
                            break
                        }
                        WAIT_TIMEOUT -> continue
                        else -> return
                    }
                }
            }
        } finally {
            Kernel32.INSTANCE.CloseHandle(eventHandle)
            Advapi32.INSTANCE.RegCloseKey(key)
        }
    }
}

private const val WINDOWS_THEME_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"
private const val WINDOWS_THEME_VALUE = "AppsUseLightTheme"
private const val WINDOWS_TEXT_SIZE_KEY = "Software\\Microsoft\\Accessibility"
private const val WINDOWS_TEXT_SIZE_VALUE = "TextScaleFactor"
private const val WINDOWS_DPI_KEY = "Control Panel\\Desktop"
private const val WINDOWS_DPI_VALUE = "LogPixels"
private const val WAIT_TIMEOUT = 0x00000102
