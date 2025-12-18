package be.ecam.companion.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

/**
 * Composant wrapper pour charger des images distantes avec Kamel.
 * Compatible avec toutes les plateformes incluant Wasm.
 */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val painterResource = asyncPainterResource(data = url)
    
    KamelImage(
        resource = painterResource,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onLoading = { progress ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        onFailure = { exception ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/**
 * Composant pour afficher un avatar utilisateur avec fallback.
 */
@Composable
fun UserAvatar(
    avatarUrl: String?,
    fallbackInitial: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (!avatarUrl.isNullOrBlank()) {
        RemoteImage(
            url = avatarUrl,
            contentDescription = "Avatar",
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        // Fallback: afficher l'initiale
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = fallbackInitial.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
