package be.ecam.companion.data

import kotlinx.browser.window

actual object OfflineCache {
    actual fun save(key: String, data: String) {
        window.localStorage.setItem("cache_$key", data)
    }
    
    actual fun load(key: String): String? {
        return window.localStorage.getItem("cache_$key")
    }
    
    actual fun clear(key: String) {
        window.localStorage.removeItem("cache_$key")
    }
    
    actual fun clearAll() {
        val keysToRemove = mutableListOf<String>()
        for (i in 0 until window.localStorage.length) {
            val key = window.localStorage.key(i)
            if (key?.startsWith("cache_") == true) {
                keysToRemove.add(key)
            }
        }
        keysToRemove.forEach { window.localStorage.removeItem(it) }
    }
}