package be.ecam.companion.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // Utilisez viewModelScope au lieu de créer votre propre scope
import be.ecam.companion.data.ApiRepository
import be.ecam.companion.data.PaeCourse
import be.ecam.companion.data.PaeRepository
import be.ecam.companion.data.PaeStudent
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate


class HomeViewModel(private val repository: ApiRepository) : ViewModel() {


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

    fun load() {
        viewModelScope.launch {
            try {
                val hello = repository.fetchHello()
                helloMessage = hello.message
            } catch (t: Throwable) {
                lastErrorMessage = friendlyErrorMessage(t)
            }
        }
        viewModelScope.launch {
            try {
                val schedule = repository.fetchSchedule()
                val parsed = schedule.mapKeys { (k, _) ->
                    val parts = k.split("-")

                    LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                }.mapValues { (_, v) -> v.map { it.title } }
                scheduledByDate = parsed
            } catch (t: Throwable) {
                lastErrorMessage = friendlyErrorMessage(t)
            }
        }
        viewModelScope.launch {
            try {
                val db = PaeRepository.load()
                val firstStudent = db.students.firstOrNull()
                student = firstStudent
                val targetRecord = firstStudent?.records?.find { record ->
                    record.academicYearLabel == "Année académique 2025-2026" || record.catalogYear == "2025-2026"
                }
                courses = targetRecord?.courses ?: emptyList()
            } catch (t: Throwable) {
                // On concatène l'erreur ou on remplace, selon la préférence.
                // Ici on remplace pour l'exemple.
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