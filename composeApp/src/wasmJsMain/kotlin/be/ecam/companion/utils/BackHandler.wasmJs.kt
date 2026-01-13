package be.ecam.companion.utils

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Web n'a pas de geste de retour système natif
    // Le bouton retour du navigateur est géré par l'historique
}