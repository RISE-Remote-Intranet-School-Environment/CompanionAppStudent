package be.ecam.companion.ui.theme

import android.content.res.Resources

internal actual fun platformScreenWidthDp(): Int? {
    val metrics = Resources.getSystem().displayMetrics ?: return null
    val density = metrics.density
    if (density == 0f) return null
    return (metrics.widthPixels / density).toInt()
}
