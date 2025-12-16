package be.ecam.companion.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
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

        val remote = fetchFromServer(base)
        val result = FormationCatalogResult(database = remote, fromServer = true)

        cachedBaseUrl = base
        cached = result
        return result
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

        val formations: List<ServerFormationDto> = client
            .get("$baseUrl/api/formations") {
                token?.let { tokenValue ->
                    header(HttpHeaders.Authorization, "Bearer $tokenValue")
                }
            }
            .body()

        val coursesByFormation = formations.associate { formation ->
            formation.formationId to async {
                client
                    .get("$baseUrl/api/courses/by-formation/${formation.formationId}") {
                        token?.let { tokenValue ->
                            header(HttpHeaders.Authorization, "Bearer $tokenValue")
                        }
                    }
                    .body<List<ServerCourseDto>>()
            }
        }.mapValues { it.value.await() }

        val mappedFormations = formations.map { formation ->
            val courses = coursesByFormation[formation.formationId].orEmpty()
            val blocks = courses
                .groupBy { course -> course.blocId?.takeIf { it.isNotBlank() } ?: "Bloc 1" }
                .map { (blocName, blocCourses) ->
                    FormationBlock(
                        name = blocName,
                        courses = blocCourses.map { it.toFormationCourse() }
                    )
                }

            Formation(
                id = formation.formationId,
                name = formation.name,
                sourceUrl = formation.sourceUrl,
                notes = null,
                blocks = blocks,
                imageKey = null,
                imageUrl = formation.imageUrl
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
private data class ServerFormationDto(
    val id: Int? = null,
    @SerialName("formationId") val formationId: String,
    val name: String,
    @SerialName("sourceUrl") val sourceUrl: String? = null,
    @SerialName("imageUrl") val imageUrl: String? = null
)

@Serializable
private data class ServerCourseDto(
    val id: Int? = null,
    @SerialName("courseId") val courseId: String,
    val title: String,
    val credits: Int? = null,
    @SerialName("blocId") val blocId: String? = null,
    val periods: String? = null,
    @SerialName("detailsUrl") val detailsUrl: String? = null
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
        detailsUrl = detailsUrl
    )
}
