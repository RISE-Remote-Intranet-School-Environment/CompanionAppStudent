package be.ecam.companion.di

import be.ecam.companion.data.ApiRepository
import be.ecam.companion.data.KtorApiRepository
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.viewmodel.HomeViewModel
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

// Simple Koin setup for Multiplatform Compose
val appModule = module {

    // Provide Ktor HttpClient with JSON plugin (base URL includes port; client itself is engine + JSON only)
    single { platformBuildHttpClient() }

    // Repository implementations
    single {
        val repo = get<SettingsRepository>()
        val baseUrlProvider = { buildBaseUrl(repo.getServerHost(), repo.getServerPort()) }
        KtorApiRepository(get(), baseUrlProvider)
    } bind ApiRepository::class


    // ViewModels
    viewModel { HomeViewModel(get<ApiRepository>()) }
}

// Helper to build base URL from host and port (force HTTP scheme)
fun buildBaseUrl(host: String, port: Int): String {
    val stripped = host.removePrefix("http://").removePrefix("https://")
        .substringBefore("/") // ignore any path if pasted
    return "http://$stripped:$port"
}

// Platform-specific client builder (Android adds Logging, others are minimal)
expect fun platformBuildHttpClient(): HttpClient

// Shared JSON configuration for all platforms
val appJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
