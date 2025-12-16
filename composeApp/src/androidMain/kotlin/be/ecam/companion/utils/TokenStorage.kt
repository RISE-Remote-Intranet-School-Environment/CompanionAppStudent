package be.ecam.companion.utils

// Pour Android, idéalement on utiliserait SharedPreferences ou DataStore.
// Pour l'instant, on laisse vide pour que ça compile.
actual fun saveToken(token: String) {
    // TODO: Implémenter avec SharedPreferences
}

actual fun loadToken(): String? {
    return null
}

actual fun clearToken() {
}