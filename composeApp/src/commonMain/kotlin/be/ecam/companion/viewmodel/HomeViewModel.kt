package be.ecam.companion.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.ecam.companion.data.ApiRepository
import be.ecam.companion.data.PaeCourse
import be.ecam.companion.data.PaeRepository
import be.ecam.companion.data.PaeStudent
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

// Assurez-vous d'avoir MockApiRepository ou NetworkApiRepository disponible
class HomeViewModel(
    private val repository: ApiRepository
) : ViewModel() {

    var helloMessage by mutableStateOf("")
        private set

    var scheduledByDate by mutableStateOf<Map<LocalDate, List<String>>>(emptyMap())
        private set

    var courses by mutableStateOf<List<PaeCourse>>(emptyList())
        private set

    var student by mutableStateOf<PaeStudent?>(null)
        private set

    var lastErrorMessage by mutableStateOf("")
        private set

    /**
     * Charge les données.
     * @param userIdentifier : L'email ou le username de l'étudiant à charger.
     * Valeur par défaut mise à "nicolas.schell" pour que l'app fonctionne même sans login préalable lors des tests.
     */
    fun load(userIdentifier: String = "nicolas.schell") {
        // 1. Hello Message (API)
        viewModelScope.launch {
            try {
                val hello = repository.fetchHello()
                helloMessage = hello.message
            } catch (t: Throwable) {
                lastErrorMessage = friendlyErrorMessage(t)
            }
        }

        // 2. Schedule (API)
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

        // 3. PAE (Local JSON) - Filtrage par utilisateur et année
        viewModelScope.launch {
            try {
                val db = PaeRepository.load()

                // --- CORRECTION DE L'ERREUR ICI ---
                // On utilise le paramètre 'userIdentifier' passé à la fonction
                val targetStudent = db.students.find { student ->
                    student.username == userIdentifier || student.email == userIdentifier
                } ?: db.students.firstOrNull() // Fallback : premier étudiant si non trouvé

                student = targetStudent

                // Filtrage année 2025-2026
                val targetRecord = targetStudent?.records?.find { record ->
                    record.academicYearLabel == "2025-2026" || record.catalogYear == "2025-2026"
                }

                courses = targetRecord?.courses ?: emptyList()

            } catch (t: Throwable) {
                lastErrorMessage = t.message ?: "Erreur lors du chargement du PAE"
            }
        }
    }

    private fun friendlyErrorMessage(t: Throwable): String {
        val msg = t.message ?: ""
        val simple = t::class.simpleName ?: "Error"
        return "Cannot reach the server. Please check Settings and your connection. ($simple: $msg)"
    }
}

// Classe Mock temporaire si vous n'avez pas encore la vraie implémentation réseau accessible ici

