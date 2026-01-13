package be.ecam.companion.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event

@Composable
internal actual fun systemPrefersDarkTheme(): Boolean? {
    val mediaQuery = remember { window.matchMedia("(prefers-color-scheme: dark)") }
    val state = remember { mutableStateOf<Boolean?>(mediaQuery.matches) }

    DisposableEffect(mediaQuery) {
        val listener: (Event) -> Unit = { state.value = mediaQuery.matches }
        mediaQuery.addEventListener("change", listener)
        onDispose { mediaQuery.removeEventListener("change", listener) }
    }

    return state.value
}

@Composable
internal actual fun systemFontScale(): Float? {
    val state = remember { mutableStateOf(readFontScale()) }

    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { state.value = readFontScale() }
        window.addEventListener("resize", listener)
        onDispose { window.removeEventListener("resize", listener) }
    }

    return state.value
}

private fun readFontScale(): Float? {
    val root = document.documentElement ?: return null
    val fontSize = window.getComputedStyle(root).fontSize
    val px = fontSize.removeSuffix("px").toFloatOrNull() ?: return null
    return px / 16f
}
