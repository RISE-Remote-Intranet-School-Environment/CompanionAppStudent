package be.ecam.companion.di

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json

actual fun platformBuildHttpClient(): HttpClient = HttpClient(Android) {
    // JSON
    install(ContentNegotiation) { json() }

    // Timeouts to avoid indefinite hangs
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }

    // Verbose logging for debug builds (detected via reflection to avoid hard dependency)
    val isDebug = try {
        val clazz = Class.forName("be.ecam.companion.BuildConfig")
        val field = clazz.getField("DEBUG")
        field.getBoolean(null)
    } catch (_: Throwable) { false }
    if (isDebug) {
        install(Logging) {
            level = LogLevel.ALL
            logger = AndroidLogKtorLogger()
            sanitizeHeader { header ->
                // keep common headers visible; redact Authorization if used
                header.equals("Authorization", ignoreCase = true)
            }
        }
    }

    expectSuccess = false
}
private class AndroidLogKtorLogger : Logger {
    override fun log(message: String) {
        Log.d("KtorClient", message)
    }
}

