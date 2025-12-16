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
}