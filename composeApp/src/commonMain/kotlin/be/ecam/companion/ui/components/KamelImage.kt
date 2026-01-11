package be.ecam.companion.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

/**
 * Composant wrapper pour charger des images distantes avec Kamel.
 * Supporte les URLs HTTP et les data URLs base64.
 */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    //  Support des images base64 (data:image/...)
    if (url.startsWith("data:image")) {
        Base64Image(
            base64Data = url,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
        return
    }

    // URLs HTTP classiques -> Kamel
    val painterResource = asyncPainterResource(data = url)

    KamelImage(
        resource = { painterResource },
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
 * Affiche une image encodée en base64 (multiplateforme via expect/actual)
 */
@Composable
expect fun Base64Image(
    base64Data: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
)

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
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = fallbackInitial.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}