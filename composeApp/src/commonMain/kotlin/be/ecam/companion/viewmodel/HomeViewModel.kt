package be.ecam.companion.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.ecam.companion.data.ApiRepository
import be.ecam.companion.data.CourseDetail
import be.ecam.companion.data.CourseDetailsRepository
import be.ecam.companion.data.PaeCourse
import be.ecam.companion.data.PaeRepository
import be.ecam.companion.data.PaeStudent
import be.ecam.companion.data.defaultServerBaseUrl
import be.ecam.companion.utils.loadToken
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class HomeViewModel(
    private val repository: ApiRepository,
    private val httpClient: HttpClient
) : ViewModel() {

    var helloMessage by mutableStateOf("")
        private set

    var scheduledByDate by mutableStateOf<Map<LocalDate, List<String>>>(emptyMap())
        private set

    // Vos cours inscrits (PAE)
    var courses by mutableStateOf<List<PaeCourse>>(emptyList())
        private set

    // Catalogue complet (pour la recherche)
    var catalogCourses by mutableStateOf<List<CourseDetail>>(emptyList())
        private set

    var student by mutableStateOf<PaeStudent?>(null)
        private set

    var lastErrorMessage by mutableStateOf("")
        private set

    // 🔥 NOUVEAU : cours sélectionné pour afficher ses ressources
    var selectedCourseForResources by mutableStateOf<PaeCourse?>(null)
        private set

    fun load(userIdentifier: AuthUserDTO?) {
        val baseUrl = defaultServerBaseUrl()
        val bearer = loadToken()?.trim()?.removeSurrounding("\"")

        // 1. Hello Message
        viewModelScope.launch {
            try {
                val hello = repository.fetchHello()
                helloMessage = hello.message
            } catch (t: Throwable) {
                lastErrorMessage = friendlyErrorMessage(t)
            }
        }

        // 2. Schedule
        viewModelScope.launch {
            try {
                val schedule = repository.fetchSchedule()
                val parsed = schedule.mapKeys { (k, _) ->
                    val parts = k.split("-")
                    if (parts.size == 3) {
                        LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                    } else {
                        LocalDate(2025, 1, 1)
                    }
                }.mapValues { (_, v) -> v.map { it.title } }
                scheduledByDate = parsed
            } catch (t: Throwable) {
                lastErrorMessage = friendlyErrorMessage(t)
            }
        }

        // 3. PAE (Vos cours)
        viewModelScope.launch {
            try {
                val db = PaeRepository.load(baseUrl = baseUrl, token = bearer)
                val targetStudent = db.students.find { student ->
                    student.username == userIdentifier?.username || student.email == userIdentifier?.email
                } ?: db.students.firstOrNull()

                student = targetStudent

                val targetRecord = targetStudent?.records?.find { record ->
                    record.academicYearLabel == "2025-2026" || record.catalogYear == "2025-2026"
                } ?: targetStudent?.records?.firstOrNull()
                courses = targetRecord?.courses ?: emptyList()
            } catch (t: Throwable) {
                lastErrorMessage = t.message ?: "Erreur chargement PAE"
            }
        }

        // 4. Chargement du catalogue complet
        viewModelScope.launch {
            if (catalogCourses.isEmpty()) {
                try {
                    val detailsRepo = CourseDetailsRepository(
                        client = httpClient,
                        baseUrlProvider = { baseUrl },
                        authTokenProvider = { bearer }
                    )
                    catalogCourses = detailsRepo.loadAll()
                } catch (e: Exception) {
                    println("Erreur chargement catalogue: ${e.message}")
                }
            }
        }
    }

    // 🔥 NOUVELLES FONCTIONS POUR LA NAVIGATION RESSOURCES

    fun openCourseResources(course: PaeCourse) {
        selectedCourseForResources = course
    }

    fun closeCourseResources() {
        selectedCourseForResources = null
    }

    private fun friendlyErrorMessage(t: Throwable): String {
        val msg = t.message ?: ""
        val simple = t::class.simpleName ?: "Error"
        return "Cannot reach the server. ($simple: $msg)"
    }
}
