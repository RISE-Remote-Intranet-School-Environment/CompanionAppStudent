package be.ecam.companion.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import be.ecam.companion.data.ApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class HomeViewModel(private val repository: ApiRepository) : ViewModel() {
    var helloMessage by mutableStateOf("")
        private set

    var lastErrorMessage by mutableStateOf("")
        private set

    // Map date -> titles
    var scheduledByDate by mutableStateOf<Map<LocalDate, List<String>>>(emptyMap())
        private set

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    fun load() {
        scope.launch {
            try {
                val hello = repository.fetchHello()
                helloMessage = hello.message
                lastErrorMessage = ""
            } catch (t: Throwable) {
                lastErrorMessage = friendlyErrorMessage(t)
            }
        }
        scope.launch {
            try {
                val schedule = repository.fetchSchedule()
                // parse keys as LocalDate (yyyy-MM-dd)
                val parsed = schedule.mapKeys { (k, _) ->
                    val parts = k.split("-")
                    LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                }.mapValues { (_, v) -> v.map { it.title } }
                scheduledByDate = parsed
            } catch (t: Throwable) {
                lastErrorMessage = friendlyErrorMessage(t)
            }
        }
    }

    private fun friendlyErrorMessage(t: Throwable): String {
        val msg = t.message ?: ""
        val simple = t::class.simpleName ?: "Error"
        return "Cannot reach the server. Please check Settings and your connection. ($simple: $msg)"
    }
}
