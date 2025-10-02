package be.ecam.companion

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

        setContent {
            val androidModule = module {
                single<SettingsRepository> { PersistentSettingsRepository(applicationContext) }
            }
            App(extraModules = listOf(androidModule))
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
