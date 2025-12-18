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
    // Vérifier si on arrive avec des tokens OAuth dans l'URL
    val params = js("new URLSearchParams(window.location.search)")
    val accessToken = params.get("accessToken") as? String
    val refreshToken = params.get("refreshToken") as? String
    
    // Si on a des tokens, les stocker et nettoyer l'URL
    if (!accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
        window.localStorage.setItem("jwt_token", accessToken)
        window.localStorage.setItem("refresh_token", refreshToken)
        window.localStorage.setItem("oauth_success", "true")
        
        // Nettoyer l'URL (enlever les paramètres)
        window.history.replaceState(null, "", window.location.pathname)
    }
    
    ComposeViewport(document.body!!) {
        val webModule = module {
            single<SettingsRepository> { InMemorySettingsRepository() }
        }
        App(
            extraModules = listOf(webModule),
            loginUrlGenerator = {
                // Encode l'URL de retour pour éviter les problèmes avec le pipe
                val returnUrl = js("encodeURIComponent(window.location.origin)") as String
                "${defaultServerBaseUrl()}/api/auth/microsoft/login?platform=web&returnUrl=$returnUrl"
            }
        )
    }
}
