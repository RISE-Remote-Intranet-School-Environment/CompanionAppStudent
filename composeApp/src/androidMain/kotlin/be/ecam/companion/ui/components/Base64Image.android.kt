package be.ecam.companion.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.util.Base64

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
            val bytes = Base64.decode(base64Pure, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (e: Exception) { null }
    }

    if (imageBitmap != null) {
        Image(bitmap = imageBitmap, contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { Text("?") }
    }
}