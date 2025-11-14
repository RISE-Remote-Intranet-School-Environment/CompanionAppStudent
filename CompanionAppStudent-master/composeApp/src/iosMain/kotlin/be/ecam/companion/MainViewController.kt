package be.ecam.companion

import androidx.compose.ui.window.ComposeUIViewController
import be.ecam.companion.data.PersistentSettingsRepository
import be.ecam.companion.data.SettingsRepository
import org.koin.dsl.module

fun MainViewController() = ComposeUIViewController { 
    val iosModule = module {
        single<SettingsRepository> { PersistentSettingsRepository() }
    }
    App(extraModules = listOf(iosModule))
}