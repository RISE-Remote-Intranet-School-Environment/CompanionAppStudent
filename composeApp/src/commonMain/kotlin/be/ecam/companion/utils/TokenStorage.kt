package be.ecam.companion.utils

/**
 * Sauvegarde l'access token de manière sécurisée
 */
expect fun saveToken(token: String)

/**
 * Charge l'access token
 */
expect fun loadToken(): String?

/**
 * Supprime tous les tokens
 */
expect fun clearToken()

/**
 * Sauvegarde le refresh token de manière sécurisée
 * Note: Sur le web, cette fonction peut être no-op si le refresh token
 * est géré par cookie HttpOnly
 */
expect fun saveRefreshToken(token: String)

/**
 * Charge le refresh token
 * Note: Sur le web, retourne null car le refresh token est dans un cookie HttpOnly
 */
expect fun loadRefreshToken(): String?