package be.ecam.companion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIContentSizeCategoryDidChangeNotification
import platform.UIKit.UIFont
import platform.UIKit.UIFontTextStyleBody

@Composable
internal actual fun systemPrefersDarkTheme(): Boolean? = isSystemInDarkTheme()

@Composable
internal actual fun systemFontScale(): Float? {
    val state = remember { mutableStateOf(readFontScale()) }

    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            name = UIContentSizeCategoryDidChangeNotification,
            `object` = null,
            queue = null
        ) { _ ->
            state.value = readFontScale()
        }
        onDispose { center.removeObserver(observer) }
    }

    return state.value
}

private fun readFontScale(): Float? {
    val base = 17.0
    val preferred = UIFont.preferredFontForTextStyle(UIFontTextStyleBody).pointSize
    return if (preferred > 0.0) (preferred / base).toFloat() else null
}
