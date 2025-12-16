package be.ecam.companion.utils

import platform.Foundation.NSUserDefaults

private const val TOKEN_KEY = "jwt_token"

actual fun saveToken(token: String) {
    NSUserDefaults.standardUserDefaults.setObject(token, TOKEN_KEY)
}

actual fun loadToken(): String? {
    return NSUserDefaults.standardUserDefaults.stringForKey(TOKEN_KEY)
}

actual fun clearToken() {
    NSUserDefaults.standardUserDefaults.removeObjectForKey(TOKEN_KEY)
}