package be.ecam.companion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import be.ecam.companion.data.PersistentSettingsRepository
import be.ecam.companion.data.SettingsRepository
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Gérer le Deep Link au démarrage
        handleDeepLink(intent)

        setContent {
            val androidModule = module {
                single<SettingsRepository> { PersistentSettingsRepository(applicationContext) }
            }
            App(extraModules = listOf(androidModule))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return

        // be.ecam.companion://auth-callback?accessToken=xxx&refreshToken=xxx
        if (uri.scheme == "be.ecam.companion" && uri.host == "auth-callback") {
            val accessToken = uri.getQueryParameter("accessToken")
            val refreshToken = uri.getQueryParameter("refreshToken")
            val error = uri.getQueryParameter("error")

            if (!accessToken.isNullOrBlank()) {
                // Stocker le token et notifier le ViewModel
                // Tu peux utiliser un EventBus, SharedFlow, ou passer par les préférences
                OAuthCallbackHandler.onTokenReceived(accessToken, refreshToken)
            } else if (!error.isNullOrBlank()) {
                OAuthCallbackHandler.onError(error)
            }
        }
    }
}

// Gestionnaire de callback OAuth (singleton simple)
object OAuthCallbackHandler {
    var pendingAccessToken: String? = null
    var pendingRefreshToken: String? = null
    var pendingError: String? = null

    fun onTokenReceived(accessToken: String, refreshToken: String?) {
        pendingAccessToken = accessToken
        pendingRefreshToken = refreshToken
        pendingError = null
    }

    fun onError(error: String) {
        pendingAccessToken = null
        pendingRefreshToken = null
        pendingError = error
    }

    fun consumeToken(): Pair<String, String?>? {
        val token = pendingAccessToken ?: return null
        val refresh = pendingRefreshToken
        pendingAccessToken = null
        pendingRefreshToken = null
        return token to refresh
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
