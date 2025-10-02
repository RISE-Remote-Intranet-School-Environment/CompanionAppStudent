package be.ecam.companion

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import be.ecam.companion.data.PersistentSettingsRepository
import be.ecam.companion.data.SettingsRepository
import org.koin.dsl.module

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "companion",
    ) {
        run {
            val desktopModule = module {
                single<SettingsRepository> { PersistentSettingsRepository() }
            }
            App(extraModules = listOf(desktopModule))
        }
    }
}
