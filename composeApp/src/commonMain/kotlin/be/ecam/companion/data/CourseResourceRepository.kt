package be.ecam.companion.data

import be.ecam.common.api.CourseResource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.serialization.Serializable

class CourseResourceRepository(
    private val client: HttpClient,
    private val baseUrlProvider: () -> String
) {
    suspend fun getResourcesForCourse(courseCode: String, token: String? = null): List<CourseResource> {
        val candidates = buildList {
            val trimmed = courseCode.trim()
            if (trimmed.isNotEmpty()) {
                add(trimmed)
                add(trimmed.replace(" ", ""))
                add(trimmed.lowercase())
                add(trimmed.uppercase())
                add(trimmed.replace(" ", "").lowercase())
                add(trimmed.replace(" ", "").uppercase())
            }
        }.distinct()
        if (candidates.isEmpty()) return emptyList()

        // Tentative directe par code
        for (candidate in candidates) {
            val result = runCatching {
                val response = client.get {
                    url(baseUrlProvider())
                    url { path("api", "courses", candidate, "resources") }
                    token?.let { header(HttpHeaders.Authorization, "Bearer ${it.trim().removeSurrounding("\"")}") }
                    header(HttpHeaders.Accept, "application/json")
                }

                if (response.status.isSuccess()) {
                    val dtos: List<CourseResourceDto> = response.body()
                    dtos.map { CourseResource(title = it.title, type = it.type, url = it.url) }
                } else {
                    println("Erreur course-resources (${response.status}) pour code '$candidate'")
                    emptyList()
                }
            }.getOrElse { e ->
                println("Erreur récupération course-resources pour code '$candidate': ${e.message}")
                emptyList()
            }

            if (result.isNotEmpty()) return result
        }

        // Fallback API: charger toutes les ressources et filtrer localement par code cours
        val all = fetchAllResources(token)
        if (all.isNotEmpty()) {
            val normalizedCandidates = candidates.map(::normalizeCode)
            val filtered = all.filter { dto ->
                val c = normalizeCode(dto.courseId ?: "")
                normalizedCandidates.any { it == c }
            }
            if (filtered.isNotEmpty()) {
                return filtered.map { CourseResource(title = it.title, type = it.type, url = it.url) }
            }
        }

        return emptyList()
    }

    private suspend fun fetchAllResources(token: String?): List<CourseResourceDto> {
        return runCatching {
            val response = client.get {
                url(baseUrlProvider())
                url { path("api", "course-resources") }
                token?.let { header(HttpHeaders.Authorization, "Bearer ${it.trim().removeSurrounding("\"")}") }
                header(HttpHeaders.Accept, "application/json")
            }
            if (response.status.isSuccess()) {
                response.body<List<CourseResourceDto>>()
            } else {
                println("Erreur course-resources/all (${response.status})")
                emptyList()
            }
        }.getOrElse { e ->
            println("Erreur récupération course-resources/all: ${e.message}")
            emptyList()
        }
    }

    private fun normalizeCode(raw: String): String =
        raw.trim().replace(" ", "").lowercase()
}

@Serializable
private data class CourseResourceDto(
    val id: Int,
    val professorId: Int? = null,
    val professorCode: String? = null,
    val courseId: String? = null,
    val sousCourseId: String? = null,
    val title: String,
    val type: String,
    val url: String,
    val uploadedAt: Long
)
