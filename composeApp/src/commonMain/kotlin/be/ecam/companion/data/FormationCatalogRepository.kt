package be.ecam.companion.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
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
    private val baseUrlProvider: () -> String
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
        val formations: List<ServerFormationDto> = client
            .get("$baseUrl/api/formations")
            .body()

        val coursesBySlug = formations.associate { formation ->
            formation.slug to async {
                client
                    .get("$baseUrl/api/formations/${formation.slug}/courses")
                    .body<List<ServerCourseDto>>()
            }
        }.mapValues { it.value.await() }

        val mappedFormations = formations.map { formation ->
            val courses = coursesBySlug[formation.slug].orEmpty()
            val blocks = courses
                .groupBy { course -> course.bloc?.takeIf { it.isNotBlank() } ?: "Bloc 1" }
                .map { (blocName, blocCourses) ->
                    FormationBlock(
                        name = blocName,
                        courses = blocCourses.map { it.toFormationCourse() }
                    )
                }

            Formation(
                id = formation.slug,
                name = formation.name,
                sourceUrl = formation.sourceUrl,
                notes = null,
                blocks = blocks,
                imageKey = formation.imageKey,
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
    val slug: String,
    val name: String,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("image_key") val imageKey: String? = null,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
private data class ServerCourseDto(
    val id: Int? = null,
    val code: String,
    val title: String,
    val credits: Int? = null,
    val bloc: String? = null,
    val periods: String? = null,
    @SerialName("details_url") val detailsUrl: String? = null
)

private fun ServerCourseDto.toFormationCourse(): FormationCourse {
    val periodsList = periods
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    return FormationCourse(
        code = code,
        title = title,
        credits = credits?.toDouble() ?: 0.0,
        periods = periodsList,
        detailsUrl = detailsUrl
    )
}
