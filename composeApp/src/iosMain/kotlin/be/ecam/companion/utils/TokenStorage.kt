package be.ecam.companion.utils

import platform.Foundation.NSUserDefaults
import platform.Security.*
import kotlinx.cinterop.*

private const val ACCESS_TOKEN_KEY = "jwt_token"
private const val REFRESH_TOKEN_KEY = "refresh_token"
private const val SERVICE_NAME = "be.ecam.companion"

actual fun saveToken(token: String) {
    saveToKeychain(ACCESS_TOKEN_KEY, token)
}

actual fun loadToken(): String? {
    return loadFromKeychain(ACCESS_TOKEN_KEY)
}

actual fun clearToken() {
    deleteFromKeychain(ACCESS_TOKEN_KEY)
    deleteFromKeychain(REFRESH_TOKEN_KEY)
}

actual fun saveRefreshToken(token: String) {
    saveToKeychain(REFRESH_TOKEN_KEY, token)
}

actual fun loadRefreshToken(): String? {
    return loadFromKeychain(REFRESH_TOKEN_KEY)
}

@OptIn(ExperimentalForeignApi::class)
private fun saveToKeychain(key: String, value: String) {
    deleteFromKeychain(key)
    
    val data = value.encodeToByteArray().toNSData()
    val query = mapOf<Any?, Any?>(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to SERVICE_NAME,
        kSecAttrAccount to key,
        kSecValueData to data,
        kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly
    ).toNSDictionary()
    
    SecItemAdd(query, null)
}

@OptIn(ExperimentalForeignApi::class)
private fun loadFromKeychain(key: String): String? {
    memScoped {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to key,
            kSecReturnData to true,
            kSecMatchLimit to kSecMatchLimitOne
        ).toNSDictionary()
        
        val result = alloc<ObjCObjectVar<Any?>>()
        val status = SecItemCopyMatching(query, result.ptr)
        
        if (status == errSecSuccess) {
            val data = result.value as? NSData ?: return null
            return data.toByteArray().decodeToString()
        }
        return null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun deleteFromKeychain(key: String) {
    val query = mapOf<Any?, Any?>(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to SERVICE_NAME,
        kSecAttrAccount to key
    ).toNSDictionary()
    
    SecItemDelete(query)
}

// Extensions helpers
private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.dataWithBytes(this@toNSData.refTo(0), this@toNSData.size.toULong())
}

private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).apply {
    memScoped {
        getBytes(this@apply.refTo(0), length)
    }
}

private fun Map<Any?, Any?>.toNSDictionary(): NSDictionary {
    return NSDictionary.dictionaryWithObjects(values.toList(), keys.toList())
}