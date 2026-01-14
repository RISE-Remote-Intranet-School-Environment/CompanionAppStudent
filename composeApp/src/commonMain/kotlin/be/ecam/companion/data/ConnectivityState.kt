package be.ecam.companion.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Singleton pour suivre l'état de connectivité de l'application.
 * Mis à jour automatiquement par les repositories lors des requêtes réseau.
 */
object ConnectivityState {
    var isOffline by mutableStateOf(false)
        private set
    
    var lastError by mutableStateOf<String?>(null)
        private set

    /**
     * Signale une erreur réseau (appelé par les repositories)
     */
    fun reportNetworkError(error: String?) {
        isOffline = true
        lastError = error
    }

    /**
     * Signale une requête réussie
     */
    fun reportSuccess() {
        isOffline = false
        lastError = null
    }
}