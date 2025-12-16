package be.ecam.companion.utils

import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userNodeForPackage(TokenStorageHelper::class.java)
private const val TOKEN_KEY = "jwt_token"

// Objet factice pour obtenir le package
private object TokenStorageHelper

actual fun saveToken(token: String) {
    prefs.put(TOKEN_KEY, token)
}

actual fun loadToken(): String? {
    return prefs.get(TOKEN_KEY, null)
}

actual fun clearToken() {
    prefs.remove(TOKEN_KEY)
}