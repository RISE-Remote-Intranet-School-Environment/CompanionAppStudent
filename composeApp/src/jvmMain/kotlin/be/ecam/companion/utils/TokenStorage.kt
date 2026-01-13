package be.ecam.companion.utils

import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userNodeForPackage(TokenStorageHelper::class.java)
private const val TOKEN_KEY = "jwt_token"
private const val REFRESH_TOKEN_KEY = "refresh_token"

private object TokenStorageHelper

actual fun saveToken(token: String) {
    prefs.put(TOKEN_KEY, token)
}

actual fun loadToken(): String? {
    return prefs.get(TOKEN_KEY, null)
}

actual fun clearToken() {
    prefs.remove(TOKEN_KEY)
    prefs.remove(REFRESH_TOKEN_KEY)
}

actual fun saveRefreshToken(token: String) {
    prefs.put(REFRESH_TOKEN_KEY, token)
}

actual fun loadRefreshToken(): String? {
    return prefs.get(REFRESH_TOKEN_KEY, null)
}