package be.ecam.companion.utils

import kotlinx.browser.window

actual fun saveToken(token: String) {
    window.localStorage.setItem("jwt_token", token)
}

actual fun loadToken(): String? {
    val token = window.localStorage.getItem("jwt_token")
    return if (token.isNullOrBlank()) null else token
}

actual fun clearToken() {
    window.localStorage.removeItem("jwt_token")
    window.localStorage.removeItem("oauth_success")
}

fun checkOAuthSuccess(): Boolean {
    val success = window.localStorage.getItem("oauth_success")
    if (success == "true") {
        window.localStorage.removeItem("oauth_success")
        return true
    }
    return false
}

// Pour le web, le refresh token est dans un cookie HttpOnly
// Cette fonction est conservée pour la compatibilité de l'interface
actual fun saveRefreshToken(token: String) {
    // Ne rien faire - le serveur gère le cookie
}

actual fun loadRefreshToken(): String? {
    // Le refresh token n'est pas accessible en JS (HttpOnly)
    // Le client doit appeler /api/auth/refresh directement
    return null
}