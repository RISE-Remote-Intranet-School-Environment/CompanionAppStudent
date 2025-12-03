package be.ecam.companion.ui.theme

enum class TextScaleMode(val fontScale: Float) {
    XS(0.6f),
    SMALL(0.8f),
    NORMAL(1f),
    LARGE(1.2f),
    XL(1.4f);

    fun next(): TextScaleMode = entries[(ordinal + 1) % entries.size]

    fun description(): String = when (this) {
        XS -> "Taille de texte: x-small"
        SMALL -> "Taille de texte: small"
        NORMAL -> "Taille de texte: normal"
        LARGE -> "Taille de texte: large"
        XL -> "Taille de texte: x-large"
    }
}
