package be.ecam.companion.data

import companion.composeapp.generated.resources.Res
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Serializable
data class ProfessorDatabase(
    val year: String,
    @SerialName("generated_at") val generatedAt: String,
    val source: String,
    val professors: List<Professor>
)

@Serializable
data class Professor(
    val id: Int,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String,
    val speciality: String,
    val office: String? = null,
    val courses: List<ProfessorCourse> = emptyList(), // tu l'utiliseras plus tard quand tu auras fait le scraping des cours
    @SerialName("photoUrl") val photoUrl: String? = null,
    val phone: String? = null,
    val roleTitle: String? = null,
    val roleDetail: String? = null,
    val diplomas: String? = null
)

@Serializable
data class ProfessorCourse(
    val code: String,
    val title: String,
    @SerialName("details_url") val detailsUrl: String? = null,

    // Champs ajoutés automatiquement par ton script d’enrichissement
    val bloc: String? = null,
    val mandatory: Boolean = false
)

object EcamProfessorsRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: ProfessorDatabase? = null

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): ProfessorDatabase {
        cache?.let { return it }
        val bytes = Res.readBytes("files/ecam_professors_2025.json")
        val data = json.decodeFromString<ProfessorDatabase>(bytes.decodeToString())
        cache = data
        return data
    }
}

data class ProfessorCatalogResult(
    val database: ProfessorDatabase,
    val fromServer: Boolean = true
)

/**
 * Loads professors exclusively from the server API (/api/professors).
 * A small cache avoids unnecessary network calls when the base URL does not change.
 */
class ProfessorCatalogRepository(
    private val client: HttpClient,
    private val baseUrlProvider: () -> String,
    private val authTokenProvider: () -> String? = { null }
) {
    private val mutex = Mutex()
    private var cached: ProfessorCatalogResult? = null
    private var cachedBaseUrl: String? = null

    suspend fun load(): ProfessorCatalogResult = mutex.withLock {
        val base = baseUrlProvider().removeSuffix("/")

        cached?.let { result ->
            if (cachedBaseUrl == base && result.fromServer) return result
        }

        val remoteDb = fetchFromServer(base)
        val result = ProfessorCatalogResult(database = remoteDb, fromServer = true)
        cachedBaseUrl = base
        cached = result
        result
    }

    suspend fun refresh(): ProfessorCatalogResult {
        mutex.withLock {
            cached = null
            cachedBaseUrl = null
        }
        return load()
    }

    private suspend fun fetchFromServer(baseUrl: String): ProfessorDatabase {
        val token = authTokenProvider()
            ?.trim()
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() }

        val response = client.get("$baseUrl/api/professors") {
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }

        if (!response.status.isSuccess()) {
            val raw = response.bodyAsText()
            val trimmed = raw.trim()
            val snippet = trimmed.take(200)
            val hint = when {
                snippet.isBlank() -> "HTTP ${response.status.value} (${response.status.description})"
                snippet.trimStart().startsWith("<") -> "HTTP ${response.status.value} (${response.status.description})"
                else -> snippet
            }
            throw IllegalStateException("Chargement des professeurs impossible : $hint")
        }

        val responseItems: List<ServerProfessorDto> = response.body()
        val courses: List<ProfessorCourseDtoRemote> = client.get("$baseUrl/api/courses") {
            token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }.body()
        val courseMap: Map<String, ProfessorCourseDtoRemote> = mutableMapOf<String, ProfessorCourseDtoRemote>().apply {
            courses.forEach { c ->
                put(c.courseId.lowercase(), c)
                c.courseRaccourciId?.let { put(it.lowercase(), c) }
            }
        }

        return ProfessorDatabase(
            year = "server",
            generatedAt = "",
            source = baseUrl,
            professors = responseItems.map { it.toProfessor(courseMap) }
        )
    }
}

@Serializable
private data class ServerProfessorDto(
    val id: Int? = null,
    @SerialName("professorId") val professorId: String? = null,
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    val email: String,
    @SerialName("roomIds") val roomIds: String? = null,
    @SerialName("phone") val phone: String? = null,
    val speciality: String? = null,
    @SerialName("fullName") val fullName: String? = null,
    @SerialName("coursesId") val coursesId: String? = null,
    @SerialName("photoUrl") val photoUrl: String? = null,
    @SerialName("roleTitle") val roleTitle: String? = null,
    @SerialName("roleDetail") val roleDetail: String? = null,
    val diplomas: String? = null
)

@Serializable
internal data class ProfessorCourseDtoRemote(
    @SerialName("courseId") val courseId: String,
    @SerialName("courseRaccourciId") val courseRaccourciId: String? = null,
    val title: String,
    @SerialName("detailsUrl") val detailsUrl: String? = null
)

private fun ServerProfessorDto.toProfessor(courseMap: Map<String, ProfessorCourseDtoRemote>): Professor {
    val parsedCourses = coursesId
        ?.removePrefix("[")
        ?.removeSuffix("]")
        ?.split(',', ';')
        ?.map { it.trim().trim('"', '\'') }
        ?.filter { it.isNotBlank() }
        ?.map { code ->
            val match = courseMap[code.lowercase()]
            ProfessorCourse(
                code = match?.courseId ?: code,
                title = match?.title ?: code,
                detailsUrl = match?.detailsUrl
            )
        }
        ?: emptyList()

    return Professor(
        id = id ?: professorId?.hashCode() ?: 0,
        firstName = firstName,
        lastName = lastName,
        email = email,
        speciality = speciality ?: "Autres",
        office = roomIds?.takeIf { it.isNotBlank() },
        courses = parsedCourses,
        photoUrl = photoUrl,
        phone = phone,
        roleTitle = roleTitle,
        roleDetail = roleDetail,
        diplomas = diplomas
    )
}
