package be.ecam.companion

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import be.ecam.companion.data.PersistentSettingsRepository
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.utils.initTokenStorage
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initTokenStorage(applicationContext)

        handleDeepLink(intent)

        setContent {
            val androidModule = module {
                single<SettingsRepository> { PersistentSettingsRepository(applicationContext) }
            }
            
            App(
                extraModules = listOf(androidModule),
                pendingOAuthResult = OAuthCallbackHandler.tokenState,
                onOAuthResultConsumed = { OAuthCallbackHandler.consumeToken() }
            )
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
            
            if (!accessToken.isNullOrBlank()) {
                OAuthCallbackHandler.onTokenReceived(accessToken, refreshToken)
            }
        }
    }
}

object OAuthCallbackHandler {
    var tokenState by mutableStateOf<Pair<String, String>?>(null)
        private set

    fun onTokenReceived(accessToken: String, refreshToken: String?) {
        tokenState = accessToken to (refreshToken ?: "")
    }

    fun consumeToken() {
        tokenState = null
    }
}
