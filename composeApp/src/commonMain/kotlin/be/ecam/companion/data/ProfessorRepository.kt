package be.ecam.companion.data

import companion.composeapp.generated.resources.Res
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
    val courses: List<ProfessorCourse> = emptyList() // tu l'utiliseras plus tard quand tu auras fait le scraping des cours
)

@Serializable
data class ProfessorCourse(
    val code: String,
    val title: String,
    @SerialName("details_url") val detailsUrl: String? = null
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
