package be.ecam.companion.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class FormationCatalogResult(
    val database: FormationDatabase,
    val fromServer: Boolean
)

/**
 * Loads the formations/blocks/courses catalog.
 * Fetches exclusively from the server (app.db).
 * A small cache avoids refetching when the base URL does not change.
 */
class FormationCatalogRepository(
    private val client: HttpClient,
    private val baseUrlProvider: () -> String,
    private val authTokenProvider: () -> String? = { null }
) {
    private val mutex = Mutex()
    private var cached: FormationCatalogResult? = null
    private var cachedBaseUrl: String? = null

    suspend fun load(): FormationCatalogResult = mutex.withLock {
        val base = baseUrlProvider().removeSuffix("/")

        // If we already have server data for this base URL, reuse it
        cached?.let { result ->
            if (cachedBaseUrl == base && result.fromServer) return result
        }

        // Tenter le réseau d'abord
        return try {
            val remote = fetchFromServer(base)
            val result = FormationCatalogResult(database = remote, fromServer = true)
            
            // Sauvegarder dans le cache offline
            CacheHelper.save(CacheKeys.FORMATIONS, remote)
            
            cachedBaseUrl = base
            cached = result
            result
        } catch (e: Exception) {
            println("Erreur réseau, tentative de chargement depuis le cache: ${e.message}")
            
            // Fallback sur le cache offline
            val cachedData = CacheHelper.load<FormationDatabase>(CacheKeys.FORMATIONS)
            if (cachedData != null) {
                val result = FormationCatalogResult(database = cachedData, fromServer = false)
                cached = result
                result
            } else {
                // Pas de cache -> renvoyer des données vides
                FormationCatalogResult(
                    database = FormationDatabase(
                        year = "",
                        generatedAt = "",
                        source = "",
                        formations = emptyList()
                    ),
                    fromServer = false
                )
            }
        }
    }

    suspend fun refresh(): FormationCatalogResult {
        mutex.withLock {
            cached = null
            cachedBaseUrl = null
        }
        return load()
    }

    private suspend fun fetchFromServer(baseUrl: String): FormationDatabase = coroutineScope {
        val token = authTokenProvider()
            ?.trim()
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() }

        val responseItems: List<FormationWithCoursesDto> = client
            .get("$baseUrl/api/formations/with-courses") {
                token?.let { tokenValue ->
                    header(HttpHeaders.Authorization, "Bearer $tokenValue")
                }
            }
            .body()

        val mappedFormations = responseItems.map { item ->
            val formation = item.formation
            val courses = item.courses

            val blocks = courses
                .groupBy { course -> course.blocId?.takeIf { it.isNotBlank() } ?: "Bloc 1" }
                .map { (blocName, blocCourses) ->
                    FormationBlock(
                        name = blocName,
                        courses = blocCourses.map { it.toFormationCourse() }
                    )
                }

            val originalUrl = formation.imageUrl
            val proxiedUrl = if (!originalUrl.isNullOrBlank() && originalUrl.startsWith("http")) {
                "$baseUrl/api/image-proxy?url=${originalUrl.encodeURLParameter()}&width=400"
            } else {
                originalUrl
            }

            Formation(
                id = formation.formationId,
                name = formation.name,
                sourceUrl = formation.sourceUrl,
                description = formation.description ?: formation.notes,
                notes = formation.notes,
                blocks = blocks,
                imageKey = null,
                imageUrl = proxiedUrl
            )
        }

        FormationDatabase(
            year = "server",
            generatedAt = "",
            source = baseUrl,
            formations = mappedFormations
        )
    }
}

@Serializable
private data class FormationWithCoursesDto(
    val formation: ServerFormationDto,
    val courses: List<ServerCourseDto>
)

@Serializable
private data class ServerFormationDto(
    val id: Int? = null,
    @SerialName("formationId") val formationId: String,
    val name: String,
    @SerialName("sourceUrl") val sourceUrl: String? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
    val description: String? = null,
    val notes: String? = null
)

@Serializable
private data class ServerCourseDto(
    val id: Int? = null,
    @SerialName("courseId") val courseId: String,
    val title: String,
    val credits: Int? = null,
    @SerialName("blocId") val blocId: String? = null,
    val periods: String? = null,
    @SerialName("detailsUrl") val detailsUrl: String? = null,
    val icon: String? = null
)

private fun ServerCourseDto.toFormationCourse(): FormationCourse {
    val periodsList = periods
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    return FormationCourse(
        code = courseId,
        title = title,
        credits = credits?.toDouble() ?: 0.0,
        periods = periodsList,
        detailsUrl = detailsUrl,
        icon = icon
    )
}
