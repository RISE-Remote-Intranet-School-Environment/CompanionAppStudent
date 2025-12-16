package be.ecam.companion.utils

expect fun saveToken(token: String)
expect fun loadToken(): String?
expect fun clearToken()