package be.ecam.companion

import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import be.ecam.companion.data.PersistentSettingsRepository
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.data.defaultServerBaseUrl
import be.ecam.companion.oauth.DesktopOAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.dsl.module
import java.awt.Desktop
import java.net.URI
import be.ecam.companion.ui.resources.appLogoMark
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 900.dp,
        position = androidx.compose.ui.window.WindowPosition.Aligned(androidx.compose.ui.Alignment.Center) 
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "ClacOxygen - Companion Student",
        state = windowState,
        icon = painterResource(appLogoMark())
    ) {
        val desktopModule = module {
            single<SettingsRepository> { PersistentSettingsRepository() }
        }
        
        // État pour stocker les tokens OAuth reçus
        var pendingOAuthTokens by remember { mutableStateOf<Pair<String, String>?>(null) }
        val scope = rememberCoroutineScope()
        
        App(
            extraModules = listOf(desktopModule),
            loginUrlGenerator = {
                // Pour Desktop, on démarre un serveur local et on encode son URL
                val localPort = DesktopOAuthHelper.start()
                val localCallbackUrl = "http://localhost:$localPort/callback"
                val encodedCallback = java.net.URLEncoder.encode(localCallbackUrl, "UTF-8")
                
                // Lancer l'attente du callback en background
                scope.launch(Dispatchers.IO) {
                    val result = DesktopOAuthHelper.waitForCallback()
                    if (result != null && result.accessToken != null && result.refreshToken != null) {
                        pendingOAuthTokens = result.accessToken to result.refreshToken
                    }
                }
                
                "${defaultServerBaseUrl()}/api/auth/microsoft/login?platform=desktop&localCallback=$encodedCallback"
            },
            // Passer les tokens OAuth pour la restauration de session
            pendingOAuthResult = pendingOAuthTokens,
            onOAuthResultConsumed = { pendingOAuthTokens = null }
        )
    }
}
