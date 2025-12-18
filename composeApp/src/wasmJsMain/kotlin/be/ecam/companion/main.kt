package be.ecam.companion

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import be.ecam.companion.data.InMemorySettingsRepository
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.data.defaultServerBaseUrl
import kotlinx.browser.document
import kotlinx.browser.window
import org.koin.dsl.module

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        run {
                val webModule = module {
                    single<SettingsRepository> { InMemorySettingsRepository() }
                }
                App(
                    extraModules = listOf(webModule),
                    loginUrlGenerator = {
                        // On passe l'origine actuelle (ex: http://localhost:8080) au backend
                        "${defaultServerBaseUrl()}/api/auth/microsoft/login?platform=web|${window.location.origin}"
                    }
                )
            }
    }
}
