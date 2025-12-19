package be.ecam.companion.data

import be.ecam.common.api.HelloResponse
import be.ecam.common.api.ScheduleItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess // 🔥 AJOUTER CET IMPORT
import be.ecam.companion.utils.loadToken

class KtorApiRepository(
    private val client: HttpClient,
    private val baseUrlProvider: () -> String
) : ApiRepository {

    override suspend fun getHello(): String {
        return try {
            val response = client.get("${baseUrlProvider()}/api/hello")
            if (response.status.isSuccess()) { // 🔥 Maintenant ça fonctionne avec l'import
                val body: Map<String, String> = response.body()
                body["message"] ?: "Hello!"
            } else {
                "Hello!"
            }
        } catch (e: Exception) {
            "Hello!"
        }
    }

    private fun baseUrl() = baseUrlProvider()
    private fun bearer() = loadToken()?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }

    override suspend fun fetchHello(): HelloResponse {
        return client.get("${baseUrl()}/api/hello") {
            bearer()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }.body()
    }

    override suspend fun fetchSchedule(): Map<String, List<ScheduleItem>> {
        return client.get("${baseUrl()}/api/schedule") {
            bearer()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }.body()
    }
}