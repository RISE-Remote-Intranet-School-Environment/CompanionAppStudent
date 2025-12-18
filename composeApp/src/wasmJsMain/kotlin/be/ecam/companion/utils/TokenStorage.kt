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
    window.localStorage.removeItem("refresh_token")
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

fun loadRefreshToken(): String? {
    return window.localStorage.getItem("refresh_token")
}