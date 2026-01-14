package be.ecam.companion.data

import android.content.Context
import android.content.SharedPreferences

private var prefs: SharedPreferences? = null

fun initOfflineCache(context: Context) {
    prefs = context.getSharedPreferences("offline_cache", Context.MODE_PRIVATE)
}

actual object OfflineCache {
    actual fun save(key: String, data: String) {
        prefs?.edit()?.putString(key, data)?.apply()
    }
    
    actual fun load(key: String): String? {
        return prefs?.getString(key, null)
    }
    
    actual fun clear(key: String) {
        prefs?.edit()?.remove(key)?.apply()
    }
    
    actual fun clearAll() {
        prefs?.edit()?.clear()?.apply()
    }
}