package be.ecam.companion.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Interface pour le cache offline par plateforme
 */
expect object OfflineCache {
    fun save(key: String, data: String)
    fun load(key: String): String?
    fun clear(key: String)
    fun clearAll()
}

/**
 * Helper pour sérialiser/désérialiser avec kotlinx.serialization
 */
object CacheHelper {
    val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    inline fun <reified T> save(key: String, data: T) {
        OfflineCache.save(key, json.encodeToString(data))
    }
    
    inline fun <reified T> load(key: String): T? {
        val raw = OfflineCache.load(key) ?: return null
        return try {
            json.decodeFromString<T>(raw)
        } catch (e: Exception) {
            null
        }
    }
}

// Clés de cache
object CacheKeys {
    const val USER_COURSES = "user_courses"
    const val FORMATIONS = "formations"
    const val CALENDAR_EVENTS = "calendar_events"
    const val COURSE_SCHEDULE = "course_schedule"
    const val PROFESSORS = "professors"
    const val CURRENT_USER = "current_user"
}