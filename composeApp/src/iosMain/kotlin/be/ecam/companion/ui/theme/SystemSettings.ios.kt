package be.ecam.companion.ui.theme

import androidx.compose.runtime.Composable
import platform.UIKit.UIFont
import platform.UIKit.UIFontTextStyleBody
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceStyleDark

@Composable
internal actual fun systemPrefersDarkTheme(): Boolean? {
    val style = UITraitCollection.currentTraitCollection.userInterfaceStyle
    return style == UIUserInterfaceStyleDark
}

@Composable
internal actual fun systemFontScale(): Float? {
    val base = 17.0
    val preferred = UIFont.preferredFontForTextStyle(UIFontTextStyleBody).pointSize
    return if (preferred > 0.0) (preferred / base).toFloat() else null
}
