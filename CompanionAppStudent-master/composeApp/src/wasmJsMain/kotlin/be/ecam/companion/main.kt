package be.ecam.companion

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import be.ecam.companion.data.InMemorySettingsRepository
import be.ecam.companion.data.SettingsRepository
import kotlinx.browser.document
import org.koin.dsl.module

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        run {
                val webModule = module {
                    single<SettingsRepository> { InMemorySettingsRepository() }
                }
                App(extraModules = listOf(webModule))
            }
    }
}
