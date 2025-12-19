package be.ecam.companion.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CourseDetailsRepository(
    private val client: HttpClient,
    private val baseUrlProvider: () -> String,
    private val authTokenProvider: () -> String? = { null }
    ) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mutex = Mutex()
    private var cachedBaseUrl: String? = null
    private var cached: List<CourseDetail>? = null

    suspend fun loadAll(): List<CourseDetail> = mutex.withLock {
        val base = baseUrlProvider().removeSuffix("/")
        cached?.takeIf { cachedBaseUrl == base }?.let { return it }

        val merged = fetchFromServer(base)
        cachedBaseUrl = base
        cached = merged
        merged
    }

    suspend fun findCourse(code: String): CourseDetail? {
        val normalized = normalizeCode(code)
        return loadAll().firstOrNull { detail ->
            normalizeCode(detail.code).contains(normalized) ||
                    normalizeCode(detail.title).contains(normalized)
        }
    }

    private suspend fun fetchFromServer(baseUrl: String): List<CourseDetail> = coroutineScope {
        val token = authTokenProvider()?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }

        // 🔥 CORRECTION : On spécifie explicitement le type attendu dans body<...>()
        // au lieu de passer par une fonction générique helper qui peut perdre le type.
        
        val coursesDeferred = async { 
            client.get("$baseUrl/api/courses") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body<List<CourseDto>>() 
        }
        
        val detailsDeferred = async { 
            client.get("$baseUrl/api/course-details") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body<List<CourseDetailsDto>>() 
        }
        
        val sousCoursesDeferred = async { 
            client.get("$baseUrl/api/sous-courses") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body<List<SousCourseDto>>() 
        }
        
        val evaluationsDeferred = async { 
            client.get("$baseUrl/api/course-evaluations") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body<List<CourseEvaluationDto>>() 
        }
        
        val professorsDeferred = async { 
            client.get("$baseUrl/api/professors") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body<List<ProfessorDto>>() 
        }
        
        val blocsDeferred = async { 
            client.get("$baseUrl/api/blocs") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body<List<BlocDto>>() 
        }
        
        val formationsDeferred = async { 
            client.get("$baseUrl/api/formations") {
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body<List<FormationDto>>() 
        }

        val courses = coursesDeferred.await()
        val details = detailsDeferred.await()
        val sousCourses = sousCoursesDeferred.await()
        val evaluations = evaluationsDeferred.await()
        val professors = professorsDeferred.await()
        val blocs = blocsDeferred.await()
        val formations = formationsDeferred.await()

        mergeData(
            courses = courses,
            details = details,
            sousCourses = sousCourses,
            evaluations = evaluations,
            professors = professors,
            blocs = blocs,
            formations = formations
        )
    }

    private fun mergeData(
        courses: List<CourseDto>,
        details: List<CourseDetailsDto>,
        sousCourses: List<SousCourseDto>,
        evaluations: List<CourseEvaluationDto>,
        professors: List<ProfessorDto>,
        blocs: List<BlocDto>,
        formations: List<FormationDto>
    ): List<CourseDetail> {
        val professorNameById = professors.associate { prof ->
            prof.professorId.lowercase() to buildTeacherName(prof)
        }
        val blocNameById = blocs.associate { it.blocId to it.name }
        val formationNameById = formations.associate { it.formationId to it.name }

        val sousCourseById = sousCourses.associateBy { it.sousCourseId }
        val sousCoursesByCourse = sousCourses.groupBy { it.courseId }
        val evaluationsByCourse = evaluations.groupBy { it.courseId }
        val detailsByCourse = details.groupBy { it.courseId }
        val coursesById = courses.groupBy { it.courseId }

        return coursesById.map { (courseId, courseEntries) ->
            val baseCourse = courseEntries.first()
            val detail = detailsByCourse[courseId]?.firstOrNull()

            val organizedActivities = sousCoursesByCourse[courseId].orEmpty().map { sc ->
                OrganizedActivity(
                    code = sc.sousCourseId,
                    title = sc.title,
                    hours_Q1 = sc.hoursQ1.takeIf { it.isNotBlank() },
                    hours_Q2 = sc.hoursQ2.takeIf { it.isNotBlank() },
                    teachers = resolveTeacherNames(sc.teachersIds, professorNameById),
                    language = sc.language
                )
            }

            val evaluatedActivities = evaluationsByCourse[courseId].orEmpty().map { eval ->
                val linkedIds = parseIds(eval.sousCourseIds)
                val linkedTitles = linkedIds.mapNotNull { sousCourseById[it]?.title }.distinct()
                val linkedDisplay = linkedIds.mapNotNull { id ->
                    sousCourseById[id]?.let { "${it.sousCourseId} ${it.title}" }
                }

                EvaluatedActivity(
                    code = eval.evaluatedActivityId,
                    title = buildEvaluationTitle(linkedTitles, baseCourse.title, eval.evaluatedActivityId),
                    weight = formatWeight(eval.weight),
                    type_Q1 = eval.typeQ1,
                    type_Q2 = eval.typeQ2,
                    type_Q3 = eval.typeQ3,
                    teachers = resolveTeacherNames(eval.teachersIds, professorNameById),
                    language = null,
                    linked_activities = linkedDisplay
                )
            }

            val formationNames = (
                courseEntries.mapNotNull { entry -> entry.formationId?.let { formationNameById[it] } } +
                        parseIds(detail?.formationIds).mapNotNull { formationNameById[it] }
                )
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

            val hours = formatHours(detail?.hoursQ1, detail?.hoursQ2)
            val sections = buildSections(detail)
            val blocDisplay = detail?.blocId?.let { blocNameById[it] } ?: baseCourse.blocId?.let { blocNameById[it] }
            val responsableName = resolveTeacherNames(detail?.responsable, professorNameById).firstOrNull()

            CourseDetail(
                code = baseCourse.courseId,
                title = baseCourse.title,
                credits = baseCourse.credits?.toString(),
                hours = hours,
                mandatory = baseCourse.mandatory,
                bloc = blocDisplay ?: detail?.blocId,
                program = formationNames.joinToString(", ").ifBlank { null },
                responsable = responsableName,
                language = baseCourse.language.lowercase(),
                organized_activities = organizedActivities,
                evaluated_activities = evaluatedActivities,
                sections = sections
            )
        }.sortedBy { it.code }
    }

    private fun buildSections(detail: CourseDetailsDto?): Map<String, String> {
        if (detail == null) return emptyMap()
        val sections = linkedMapOf<String, String>()
        detail.contribution?.takeIf { it.isNotBlank() }?.let { sections["Contribution au programme"] = it }
        detail.learningOutcomes?.takeIf { it.isNotBlank() }?.let { sections["Acquis d’apprentissage spécifiques"] = it }
        detail.content?.takeIf { it.isNotBlank() }?.let { sections["Description du contenu"] = it }
        detail.teachingMethods?.takeIf { it.isNotBlank() }?.let { sections["Méthodes d'enseignement"] = it }
        detail.evaluationMethods?.takeIf { it.isNotBlank() }?.let { sections["Méthodes d'évaluation"] = it }
        detail.courseMaterial?.takeIf { it.isNotBlank() }?.let { sections["Support de cours"] = it }
        detail.bibliography?.takeIf { it.isNotBlank() }?.let { sections["Bibliographie"] = it }
        return sections
    }

    private fun buildEvaluationTitle(linkedTitles: List<String>, courseTitle: String, fallback: String): String {
        if (linkedTitles.isEmpty()) return fallback.ifBlank { courseTitle }
        val cleaned = linkedTitles.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val joined = joinWithEt(cleaned)
        return if (joined.contains(courseTitle, ignoreCase = true)) joined else "$courseTitle — $joined"
    }

    private fun joinWithEt(items: List<String>): String = when (items.size) {
        0 -> ""
        1 -> items.first()
        2 -> "${items[0]} et ${items[1]}"
        else -> items.dropLast(1).joinToString(", ") + " et ${items.last()}"
    }

    private fun resolveTeacherNames(rawIds: String?, lookup: Map<String, String>): List<String> =
        parseIds(rawIds).map { id ->
            lookup[id.lowercase()] ?: id
        }

    private fun formatWeight(weight: Double?): String? {
        if (weight == null) return null
        return if (weight % 1.0 == 0.0) weight.toInt().toString() else weight.toString()
    }

    private fun formatHours(q1: String?, q2: String?): String? {
        val part1 = q1?.takeIf { it.isNotBlank() }?.let { "Q1 ${ensureHoursSuffix(it)}" }
        val part2 = q2?.takeIf { it.isNotBlank() }?.let { "Q2 ${ensureHoursSuffix(it)}" }
        return when {
            part1 != null && part2 != null -> "$part1 | $part2"
            part1 != null -> part1
            part2 != null -> part2
            else -> null
        }
    }

    private fun ensureHoursSuffix(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.lowercase().endsWith("h")) trimmed else "${trimmed}h"
    }

    private fun buildTeacherName(prof: ProfessorDto): String {
        val last = prof.lastName.takeIf { it.isNotBlank() }?.uppercase()
        val first = prof.firstName.takeIf { it.isNotBlank() }
        return listOfNotNull(last, first).joinToString(" ").ifBlank { prof.professorId }
    }

    private fun parseIds(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw
            .replace("[", "")
            .replace("]", "")
            .split(';', ',', '/', '|')
            .map { it.trim().trim('"', '\'') }
            .filter { it.isNotBlank() }
    }

    private fun normalizeCode(value: String): String = value.lowercase().replace(" ", "")
}

@Serializable
private data class CourseDto(
    @SerialName("courseId") val courseId: String,
    @SerialName("courseRaccourciId") val courseRaccourciId: String? = null,
    val title: String,
    val credits: Int? = null,
    val periods: String? = null,
    val detailsUrl: String? = null,
    val mandatory: Boolean = false,
    @SerialName("blocId") val blocId: String? = null,
    @SerialName("formationId") val formationId: String? = null,
    val language: String = "fr"
)

@Serializable
private data class CourseDetailsDto(
    val id: Int,
    @SerialName("courseId") val courseId: String,
    val responsable: String? = null,
    @SerialName("sousCourseId") val sousCourseId: String? = null,
    @SerialName("teachersRawId") val teachersRawId: String? = null,
    @SerialName("formationIds") val formationIds: String? = null,
    val periods: String? = null,
    @SerialName("hoursQ1") val hoursQ1: String? = null,
    @SerialName("hoursQ2") val hoursQ2: String? = null,
    val contribution: String? = null,
    @SerialName("learningOutcomes") val learningOutcomes: String? = null,
    val content: String? = null,
    @SerialName("teachingMethods") val teachingMethods: String? = null,
    @SerialName("evaluationMethods") val evaluationMethods: String? = null,
    @SerialName("courseMaterial") val courseMaterial: String? = null,
    val bibliography: String? = null,
    @SerialName("blocId") val blocId: String? = null
)

@Serializable
private data class SousCourseDto(
    val id: Int,
    @SerialName("sousCourseId") val sousCourseId: String,
    @SerialName("courseId") val courseId: String,
    val title: String,
    @SerialName("hoursQ1") val hoursQ1: String,
    @SerialName("hoursQ2") val hoursQ2: String,
    @SerialName("teachersIds") val teachersIds: String,
    val language: String
)

@Serializable
private data class CourseEvaluationDto(
    val id: Int,
    @SerialName("evaluatedActivityId") val evaluatedActivityId: String,
    @SerialName("courseId") val courseId: String,
    val weight: Double? = null,
    @SerialName("typeQ1") val typeQ1: String? = null,
    @SerialName("typeQ2") val typeQ2: String? = null,
    @SerialName("typeQ3") val typeQ3: String? = null,
    @SerialName("sousCourseIds") val sousCourseIds: String? = null,
    @SerialName("teachersIds") val teachersIds: String? = null
)

@Serializable
private data class ProfessorDto(
    val id: Int,
    @SerialName("professorId") val professorId: String,
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String
)

@Serializable
private data class BlocDto(
    val id: Int,
    @SerialName("blocId") val blocId: String,
    val name: String,
    @SerialName("formationIds") val formationIds: String? = null
)

@Serializable
private data class FormationDto(
    val id: Int,
    @SerialName("formationId") val formationId: String,
    val name: String
)
