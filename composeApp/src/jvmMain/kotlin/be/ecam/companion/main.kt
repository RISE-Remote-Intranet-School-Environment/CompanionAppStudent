package be.ecam.companion

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import be.ecam.companion.data.PersistentSettingsRepository
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.data.defaultServerBaseUrl
import be.ecam.companion.coil.initDesktopCoilImageLoader
import be.ecam.companion.oauth.DesktopOAuthHelper
import org.koin.dsl.module
import java.awt.Desktop
import java.net.URI

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "companion",
    ) {
        initDesktopCoilImageLoader()

        val desktopModule = module {
            single<SettingsRepository> { PersistentSettingsRepository() }
        }

        App(
            extraModules = listOf(desktopModule),
            loginUrlGenerator = {
                // Pour Desktop, on démarre un serveur local et on encode son URL
                val localPort = DesktopOAuthHelper.start()
                val localCallbackUrl = "http://localhost:$localPort/callback"
                val encodedCallback = java.net.URLEncoder.encode(localCallbackUrl, "UTF-8")

                // On passe l'URL du callback local au backend
                "${defaultServerBaseUrl()}/api/auth/microsoft/login?platform=desktop&localCallback=$encodedCallback"
            }
        )
    }
}
