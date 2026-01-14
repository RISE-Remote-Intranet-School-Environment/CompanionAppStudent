package be.ecam.companion.data

import java.util.prefs.Preferences

private val prefs: Preferences = Preferences.userNodeForPackage(OfflineCache::class.java)

actual object OfflineCache {
    actual fun save(key: String, data: String) {
        // Preferences a une limite de taille, découper si nécessaire
        if (data.length > Preferences.MAX_VALUE_LENGTH) {
            // Pour les gros JSON, utiliser un fichier
            val file = java.io.File(System.getProperty("user.home"), ".clacoxygen/cache/$key.json")
            file.parentFile?.mkdirs()
            file.writeText(data)
            prefs.put(key, "FILE:${file.absolutePath}")
        } else {
            prefs.put(key, data)
        }
    }
    
    actual fun load(key: String): String? {
        val value = prefs.get(key, null) ?: return null
        return if (value.startsWith("FILE:")) {
            val file = java.io.File(value.removePrefix("FILE:"))
            if (file.exists()) file.readText() else null
        } else {
            value
        }
    }
    
    actual fun clear(key: String) {
        val value = prefs.get(key, null)
        if (value?.startsWith("FILE:") == true) {
            java.io.File(value.removePrefix("FILE:")).delete()
        }
        prefs.remove(key)
    }
    
    actual fun clearAll() {
        prefs.keys().forEach { clear(it) }
    }
}