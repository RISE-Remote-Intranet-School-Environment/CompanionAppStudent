package be.ecam.companion.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.skia.Image as SkiaImage
import java.util.Base64

@Composable
actual fun Base64Image(
    base64Data: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val imageBitmap = remember(base64Data) {
        try {
            val base64Pure = base64Data.substringAfter("base64,", "")
            if (base64Pure.isBlank()) return@remember null
            
            val bytes = Base64.getDecoder().decode(base64Pure)
            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (e: Exception) {
            println(" Erreur décodage base64: ${e.message}")
            null
        }
    }

    if (imageBitmap != null) {
        Image(bitmap = imageBitmap, contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("?", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}