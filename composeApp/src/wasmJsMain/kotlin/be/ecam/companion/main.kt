package be.ecam.companion

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import be.ecam.companion.data.InMemorySettingsRepository
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.data.defaultServerBaseUrl
import kotlinx.browser.document
import kotlinx.browser.window
import org.koin.dsl.module

// Déclaration de la fonction JS globale pour l'encodage d'URL
external fun encodeURIComponent(str: String): String

// Helper pour récupérer les paramètres d'URL via JS directement
// Cela évite les problèmes de typage entre String Kotlin et JsAny du constructeur URLSearchParams
fun getSearchParam(key: String): String? = js("new URLSearchParams(window.location.search).get(key)")

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Vérifier si on arrive avec des tokens OAuth dans l'URL
    val accessToken = getSearchParam("accessToken")
    val refreshToken = getSearchParam("refreshToken")
    
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
                val returnUrl = encodeURIComponent(window.location.origin)
                "${defaultServerBaseUrl()}/api/auth/microsoft/login?platform=web&returnUrl=$returnUrl"
            },
            // Navigation dans la même fenêtre pour OAuth
            navigateToUrl = { url ->
                window.location.href = url
            },
            // Pas de pendingOAuthResult pour Wasm (géré via URL params)
            pendingOAuthResult = null,
            onOAuthResultConsumed = null
        )
    }
}
