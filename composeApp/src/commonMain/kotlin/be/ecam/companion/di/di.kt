package be.ecam.companion.di

import be.ecam.companion.data.ApiRepository
import be.ecam.companion.data.CalendarRepository
import be.ecam.companion.data.KtorApiRepository
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.viewmodel.HomeViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

// Simple Koin setup for Multiplatform Compose
val appModule = module {

    // Provide Ktor HttpClient
    single {
        platformBuildHttpClient().config {
            // On installe le plugin JSON sur le client global
            install(ContentNegotiation) {
                json(appJson)
            }
        }
    }

    // Repository implementations
    single {
        val repo = get<SettingsRepository>()
        val baseUrlProvider = { buildBaseUrl(repo.getServerHost(), repo.getServerPort()) }
        KtorApiRepository(get(), baseUrlProvider)
    } bind ApiRepository::class

    // Calendar Repository
    single {
        val repo = get<SettingsRepository>()
        val baseUrlProvider = { buildBaseUrl(repo.getServerHost(), repo.getServerPort()) }
        CalendarRepository(get(), baseUrlProvider)
    }

    // ViewModels
    viewModel { HomeViewModel(get<ApiRepository>()) }
}

// Helper to build base URL from host and port (force HTTP scheme)
fun buildBaseUrl(host: String, port: Int): String {
    val stripped = host.removePrefix("http://").removePrefix("https://")
        .substringBefore("/")
        .substringBefore(":") 
    
    // Si c'est le serveur de prod, utiliser HTTPS sans port explicite
    return if (stripped == "clacoxygen.msrl.be") {
        "https://$stripped"
    } else {
        "http://$stripped:$port"
    }
}

// Platform-specific client builder (Android adds Logging, others are minimal)
expect fun platformBuildHttpClient(): HttpClient

// Shared JSON configuration for all platforms
val appJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
