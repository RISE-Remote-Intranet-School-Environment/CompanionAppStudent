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
import be.ecam.companion.data.SettingsRepository
import be.ecam.companion.data.UserCoursesRepository
import be.ecam.companion.di.buildBaseUrl
import be.ecam.companion.utils.loadToken
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class HomeViewModel(
    private val repository: ApiRepository,
    private val settingsRepo: SettingsRepository,
    private val httpClient: HttpClient
) : ViewModel() {

    var helloMessage by mutableStateOf("")
        private set

    var scheduledByDate by mutableStateOf<Map<LocalDate, List<String>>>(emptyMap())
        private set

    // Cours PAE bruts (depuis le serveur)
    private var paeCourses by mutableStateOf<List<PaeCourse>>(emptyList())

    //  Cours affichés = PAE + sélection manuelle (fusionnés)
    var courses by mutableStateOf<List<PaeCourse>>(emptyList())
        private set

    // Catalogue complet (pour la recherche)
    var catalogCourses by mutableStateOf<List<CourseDetail>>(emptyList())
        private set

    var student by mutableStateOf<PaeStudent?>(null)
        private set

    var lastErrorMessage by mutableStateOf("")
        private set

    var selectedCourseForResources by mutableStateOf<PaeCourse?>(null)
        private set

    // IDs des cours sélectionnés manuellement par l'utilisateur
    var myCourseIds by mutableStateOf<Set<String>>(emptySet())
        private set

    var isAddingCourse by mutableStateOf(false)
        private set

    var feedbackMessage by mutableStateOf<String?>(null)
        private set

    private var currentToken: String? = null
    private var userCoursesRepo: UserCoursesRepository? = null
    
    //  CORRECTION : Stocker l'ID de l'utilisateur actuellement chargé
    private var loadedUserId: Int? = null

    fun load(userIdentifier: AuthUserDTO?, authToken: String? = null) {
        //  CORRECTION : Vérifier si c'est un nouvel utilisateur
        val newUserId = userIdentifier?.id
        if (newUserId != null && newUserId != loadedUserId) {
            // Nouvel utilisateur -> reset complet
            println("Nouvel utilisateur détecté (${loadedUserId} -> $newUserId), reset de l'état")
            resetState()
        }

        val baseUrl = buildBaseUrl(settingsRepo.getServerHost(), settingsRepo.getServerPort())
        val bearer = authToken?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }
            ?: loadToken()?.trim()?.removeSurrounding("\"")

        currentToken = bearer
        userCoursesRepo = UserCoursesRepository(httpClient) { baseUrl }

        viewModelScope.launch {
            try {
                helloMessage = "Bienvenue !"
            } catch (e: Exception) {
                lastErrorMessage = friendlyErrorMessage(e)
            }
        }

        //  Charger tout en parallèle puis fusionner
        viewModelScope.launch {
            try {
                coroutineScope {
                    // 1. Charger le PAE
                    val paeDeferred = async {
                        try {
                            val paeDatabase = PaeRepository.load(baseUrl = baseUrl, token = bearer)
                            val matchingStudent = paeDatabase.students.firstOrNull { stu ->
                                userIdentifier?.let { user ->
                                    stu.email.equals(user.email, ignoreCase = true) ||
                                            stu.username.equals(user.username, ignoreCase = true)
                                } ?: false
                            }
                            student = matchingStudent
                            val record = matchingStudent?.records
                                ?.sortedByDescending { it.catalogYear ?: it.academicYearLabel ?: "" }
                                ?.firstOrNull()
                            record?.courses ?: emptyList()
                        } catch (e: Exception) {
                            println("Erreur chargement PAE: ${e.message}")
                            emptyList()
                        }
                    }

                    // 2. Charger les cours sélectionnés manuellement
                    val selectedDeferred = async {
                        try {
                            if (!bearer.isNullOrBlank()) {
                                userCoursesRepo?.getMyCourses(bearer)?.toSet() ?: emptySet()
                            } else {
                                emptySet()
                            }
                        } catch (e: Exception) {
                            println("Erreur chargement cours sélectionnés: ${e.message}")
                            emptySet()
                        }
                    }

                    // 3. Charger le catalogue (une seule fois)
                    val catalogDeferred = async {
                        try {
                            if (catalogCourses.isEmpty()) {
                                val detailsRepo = CourseDetailsRepository(
                                    client = httpClient,
                                    baseUrlProvider = { baseUrl },
                                    authTokenProvider = { bearer }
                                )
                                detailsRepo.loadAll()
                            } else {
                                catalogCourses
                            }
                        } catch (e: Exception) {
                            println("Erreur chargement catalogue: ${e.message}")
                            emptyList()
                        }
                    }

                    // Attendre tous les résultats
                    paeCourses = paeDeferred.await()
                    myCourseIds = selectedDeferred.await()
                    catalogCourses = catalogDeferred.await()

                    //  FUSIONNER : PAE + cours sélectionnés manuellement
                    courses = mergePaeAndSelectedCourses(paeCourses, myCourseIds, catalogCourses)
                    
                    //  Marquer cet utilisateur comme chargé
                    loadedUserId = newUserId
                    
                    println("Utilisateur $newUserId - Cours PAE: ${paeCourses.size}, Sélectionnés: ${myCourseIds.size}, Total affiché: ${courses.size}")
                }
            } catch (e: Exception) {
                println("Erreur chargement HomeViewModel: ${e.message}")
            }
        }
    }

    /**
     *  NOUVEAU : Reset complet de l'état lors d'un changement d'utilisateur
     */
    private fun resetState() {
        paeCourses = emptyList()
        courses = emptyList()
        myCourseIds = emptySet()
        student = null
        selectedCourseForResources = null
        loadedUserId = null
        // Note: on garde catalogCourses car c'est commun à tous les utilisateurs
    }

    /**
     *  Fusionne les cours du PAE avec les cours sélectionnés manuellement
     */
    private fun mergePaeAndSelectedCourses(
        pae: List<PaeCourse>,
        selectedIds: Set<String>,
        catalog: List<CourseDetail>
    ): List<PaeCourse> {
        // Créer un set des codes PAE pour éviter les doublons
        val paeCodes = pae.mapNotNull { it.code?.lowercase() }.toSet()

        // Créer un index du catalogue pour lookup rapide
        val catalogIndex = catalog.associateBy { it.code.lowercase() }

        // Cours du PAE
        val fromPae = pae.toMutableList()

        // Ajouter les cours sélectionnés qui ne sont pas déjà dans le PAE
        selectedIds.forEach { selectedCode ->
            val normalizedCode = selectedCode.lowercase()
            if (normalizedCode !in paeCodes) {
                // Chercher les détails dans le catalogue
                val catalogCourse = catalogIndex[normalizedCode]
                val newCourse = if (catalogCourse != null) {
                    PaeCourse(
                        code = catalogCourse.code,
                        title = catalogCourse.title,
                        ects = catalogCourse.credits?.toIntOrNull()
                    )
                } else {
                    // Fallback si pas trouvé dans le catalogue
                    PaeCourse(
                        code = selectedCode,
                        title = selectedCode,
                        ects = null
                    )
                }
                fromPae.add(newCourse)
            }
        }

        return fromPae
    }

    // Ajouter un cours à la sélection
    fun addCourse(courseCode: String) {
        val token = currentToken ?: return
        val repo = userCoursesRepo ?: return

        viewModelScope.launch {
            isAddingCourse = true
            try {
                val success = repo.addCourse(token, courseCode)
                if (success) {
                    myCourseIds = myCourseIds + courseCode.lowercase()
                    feedbackMessage = "Cours ajouté !"

                    //  Refusionner les cours
                    courses = mergePaeAndSelectedCourses(paeCourses, myCourseIds, catalogCourses)
                } else {
                    feedbackMessage = "Cours déjà ajouté"
                }
            } catch (e: Exception) {
                feedbackMessage = "Erreur: ${e.message}"
            } finally {
                isAddingCourse = false
            }
        }
    }

    // Retirer un cours de la sélection
    fun removeCourse(courseCode: String) {
        val token = currentToken ?: return
        val repo = userCoursesRepo ?: return

        viewModelScope.launch {
            isAddingCourse = true
            try {
                val success = repo.removeCourse(token, courseCode)
                if (success) {
                    myCourseIds = myCourseIds - courseCode.lowercase()
                    feedbackMessage = "Cours retiré"

                    //  Refusionner les cours
                    courses = mergePaeAndSelectedCourses(paeCourses, myCourseIds, catalogCourses)
                }
            } catch (e: Exception) {
                feedbackMessage = "Erreur: ${e.message}"
            } finally {
                isAddingCourse = false
            }
        }
    }

    // Vérifier si un cours est sélectionné (PAE ou manuel)
    fun isCourseSelected(courseCode: String): Boolean {
        val normalizedCode = courseCode.lowercase()
        // Vérifie dans les cours manuels ET dans le PAE
        return myCourseIds.contains(normalizedCode) ||
                paeCourses.any { it.code?.lowercase() == normalizedCode }
    }

    fun clearFeedback() {
        feedbackMessage = null
    }

    fun openCourseResources(course: PaeCourse) {
        selectedCourseForResources = course
    }

    fun closeCourseResources() {
        selectedCourseForResources = null
    }

    //  Force le rechargement (utile après changement de page)
    fun forceReload(userIdentifier: AuthUserDTO?, authToken: String? = null) {
        loadedUserId = null // Force le reset
        load(userIdentifier, authToken)
    }

    private fun friendlyErrorMessage(t: Throwable): String {
        val msg = t.message ?: ""
        val simple = t::class.simpleName ?: "Error"
        return "Cannot reach the server. ($simple: $msg)"
    }
}