package be.ecam.companion.utils

import kotlinx.browser.localStorage

actual fun saveToken(token: String) {
    localStorage.setItem("jwt_token", token)
}

actual fun loadToken(): String? {
    return localStorage.getItem("jwt_token")
}

actual fun clearToken() {
    localStorage.removeItem("jwt_token")
    localStorage.removeItem("refresh_token")
    localStorage.removeItem("oauth_success")
}

fun checkOAuthSuccess(): Boolean {
    val success = localStorage.getItem("oauth_success")
    if (success == "true") {
        localStorage.removeItem("oauth_success")
        return true
    }
    return false
}

fun loadRefreshToken(): String? {
    return localStorage.getItem("refresh_token")
}