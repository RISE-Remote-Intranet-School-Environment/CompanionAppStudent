package be.ecam.companion.data

import be.ecam.companion.ui.screens.loadPaeFromServer
import be.ecam.companion.data.defaultServerBaseUrl
import be.ecam.companion.utils.loadToken
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import be.ecam.companion.data.CacheHelper

@Serializable
data class PaeDatabase(
    val students: List<PaeStudent> = emptyList()
)

@Serializable
data class PaeStudent(
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("student_id") val studentId: String? = null,
    val role: String? = null,
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val records: List<PaeRecord> = emptyList()
)

@Serializable
data class PaeRecord(
    val program: String? = null,
    @SerialName("academic_year_label") val academicYearLabel: String? = null,
    @SerialName("catalog_year") val catalogYear: String? = null,
    @SerialName("formation_slug") val formationSlug: String? = null,
    @SerialName("formation_id") val formationId: Int? = null,
    val block: String? = null,
    val courses: List<PaeCourse> = emptyList()
)

@Serializable
data class PaeCourse(
    val code: String? = null,
    val title: String? = null,
    val ects: Int? = null,
    val period: String? = null,
    @SerialName("course_id") val courseId: Int? = null,
    val sessions: PaeSessions = PaeSessions(),
    val components: List<PaeComponent> = emptyList()
)

@Serializable
data class PaeSessions(
    val jan: String? = null,
    val jun: String? = null,
    val sep: String? = null
)

@Serializable
data class PaeComponent(
    val code: String? = null,
    val title: String? = null,
    val weight: String? = null,
    val sessions: PaeSessions = PaeSessions()
)

private const val CACHE_KEY_PAE = "pae_database"

object PaeRepository {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    /**
     * Charge le PAE depuis le serveur (plus de fallback JSON local).
     * baseUrl doit être passé par l'appelant (ex: buildBaseUrl(host, port)).
     */
    suspend fun load(
        baseUrl: String = defaultServerBaseUrl(),
        token: String? = null
    ): PaeDatabase {
        val resolvedToken = token?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }
            ?: loadToken()?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }
        
        return try {
            val result = loadPaeFromServer(
                client = httpClient,
                baseUrl = baseUrl,
                token = resolvedToken
            )
            // Sauvegarder dans le cache
            CacheHelper.save(CACHE_KEY_PAE, result)
            // Signaler que le réseau fonctionne
            ConnectivityState.reportSuccess()
            result
        } catch (e: Exception) {
            println("Erreur PAE: ${e.message}, chargement du cache...")
            // Signaler l'erreur réseau
            ConnectivityState.reportNetworkError(e.message)
            CacheHelper.load<PaeDatabase>(CACHE_KEY_PAE) ?: PaeDatabase(emptyList())
        }
    }
}
