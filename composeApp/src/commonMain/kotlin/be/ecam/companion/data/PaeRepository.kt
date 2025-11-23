package be.ecam.companion.data

import companion.composeapp.generated.resources.Res
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

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

object PaeRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: PaeDatabase? = null

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): PaeDatabase {
        cache?.let { return it }
        val bytes = Res.readBytes("files/pae_student.json")
        val data = json.decodeFromString<PaeDatabase>(bytes.decodeToString())
        cache = data
        return data
    }
}
