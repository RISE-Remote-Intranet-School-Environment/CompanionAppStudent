package be.ecam.companion.ui.theme

data class ScreenSizeMode(val scale: Float) {
    val percent: Int
        get() = (scale * 100).toInt()

    fun description(): String = "Zoom écran: ${percent}%"

    fun next(): ScreenSizeMode {
        val order = PRESETS
        val currentIndex = order.indexOfFirst { kotlin.math.abs(it - scale) < 0.01f }
        val nextScale = if (currentIndex == -1) order.first() else order[(currentIndex + 1) % order.size]
        return ScreenSizeMode(nextScale)
    }

    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 1.5f
        private const val SMALL_SCREEN_DP = 600

        private val BaseDefault = ScreenSizeMode(1f)

        val Default: ScreenSizeMode
            get() = defaultForScreenWidthDp(platformScreenWidthDp())
        val SmallScreenDefault = ScreenSizeMode(MIN_SCALE)

        fun defaultForScreenWidthDp(screenWidthDp: Int?): ScreenSizeMode {
            val width = screenWidthDp ?: return BaseDefault
            return if (width < SMALL_SCREEN_DP) SmallScreenDefault else BaseDefault
        }

        private val PRESETS = listOf(1f, 1.25f, 1.5f, 0.5f, 0.75f)

        fun fromScale(value: Float): ScreenSizeMode =
            ScreenSizeMode(value.coerceIn(MIN_SCALE, MAX_SCALE))
    }
}

internal expect fun platformScreenWidthDp(): Int?
