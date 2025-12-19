package be.ecam.companion.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

@Composable
actual fun Base64Image(
    base64Data: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    // TODO: Implémenter avec UIImage sur iOS
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("?")
    }
}