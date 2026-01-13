package be.ecam.companion.utils

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop n'a pas de geste de retour système
    // On pourrait intercepter Escape ici si nécessaire
}