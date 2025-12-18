package be.ecam.companion

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import be.ecam.companion.data.PersistentSettingsRepository
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.data.defaultServerBaseUrl
import be.ecam.companion.coil.initDesktopCoilImageLoader
import org.koin.dsl.module

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "companion",
    ) {
        run {
        initDesktopCoilImageLoader()

        val desktopModule = module {
            single<SettingsRepository> { PersistentSettingsRepository() }
        }
        App(
            extraModules = listOf(desktopModule),
            loginUrlGenerator = {
                "${defaultServerBaseUrl()}/api/auth/microsoft/login?platform=desktop"
            }
        )
    }
}
}
