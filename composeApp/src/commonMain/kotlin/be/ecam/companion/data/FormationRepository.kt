package be.ecam.companion.data

import io.ktor.client.HttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FormationDatabase(
    val year: String,
    @SerialName("generated_at") val generatedAt: String,
    val source: String,
    val formations: List<Formation>
)

@Serializable
data class Formation(
    val id: String,
    val name: String,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("image_key") val imageKey: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val blocks: List<FormationBlock>
)

@Serializable
data class FormationBlock(
    val name: String,
    val courses: List<FormationCourse>
)

@Serializable
data class FormationCourse(
    val code: String,
    val title: String,
    val credits: Double,
    val periods: List<String> = emptyList(),
    @SerialName("details_url") val detailsUrl: String? = null,
    val icon: String? = null
)

object EcamFormationsRepository {
    private val httpClient = HttpClient()

    /**
     * Charge les formations depuis le serveur via FormationCatalogRepository.
     */
    suspend fun load(
        baseUrl: String,
        token: String? = null
    ): FormationDatabase {
        val repo = FormationCatalogRepository(
            client = httpClient,
            baseUrlProvider = { baseUrl },
            authTokenProvider = { token }
        )
        return repo.load().database
    }
}
