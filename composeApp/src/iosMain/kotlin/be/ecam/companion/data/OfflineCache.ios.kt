package be.ecam.companion.data

import platform.Foundation.NSUserDefaults

actual object OfflineCache {
    private val defaults = NSUserDefaults.standardUserDefaults
    private const val PREFIX = "cache_"
    
    actual fun save(key: String, data: String) {
        defaults.setObject(data, forKey = "$PREFIX$key")
        defaults.synchronize()
    }
    
    actual fun load(key: String): String? {
        return defaults.stringForKey("$PREFIX$key")
    }
    
    actual fun clear(key: String) {
        defaults.removeObjectForKey("$PREFIX$key")
        defaults.synchronize()
    }
    
    actual fun clearAll() {
        val dict = defaults.dictionaryRepresentation()
        dict.keys.filterIsInstance<String>()
            .filter { it.startsWith(PREFIX) }
            .forEach { defaults.removeObjectForKey(it) }
        defaults.synchronize()
    }
}