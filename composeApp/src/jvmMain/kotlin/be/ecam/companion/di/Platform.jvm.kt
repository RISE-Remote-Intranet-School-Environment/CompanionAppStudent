package be.ecam.companion.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import be.ecam.companion.di.appJson

actual fun platformBuildHttpClient(): HttpClient = HttpClient(Java) {
    install(ContentNegotiation) { json(appJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = 5_000
        socketTimeoutMillis = 5_000
        requestTimeoutMillis = 7_000
    }
    expectSuccess = false
}
