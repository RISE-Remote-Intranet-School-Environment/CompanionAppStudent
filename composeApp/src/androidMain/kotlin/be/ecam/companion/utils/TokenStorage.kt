package be.ecam.companion.utils

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "companion_auth"
private const val TOKEN_KEY = "jwt_token"

@Volatile
private var appContext: Context? = null

fun initTokenStorage(context: Context) {
    appContext = context.applicationContext
}

private fun prefs(): SharedPreferences? =
    appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

actual fun saveToken(token: String) {
    prefs()?.edit()?.putString(TOKEN_KEY, token)?.apply()
}

actual fun loadToken(): String? {
    return prefs()?.getString(TOKEN_KEY, null)
}

actual fun clearToken() {
    prefs()?.edit()?.remove(TOKEN_KEY)?.apply()
}
