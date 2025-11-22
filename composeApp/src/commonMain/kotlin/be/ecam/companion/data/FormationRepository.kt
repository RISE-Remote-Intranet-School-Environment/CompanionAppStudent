package be.ecam.companion.data

import companion.composeapp.generated.resources.Res
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

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
    @SerialName("details_url") val detailsUrl: String? = null
)

object EcamFormationsRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: FormationDatabase? = null

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): FormationDatabase {
        cache?.let { return it }
        val bytes = Res.readBytes("files/ecam_formations_2025.json")
        val data = json.decodeFromString<FormationDatabase>(bytes.decodeToString())
        cache = data
        return data
    }
}
