package be.ecam.companion.utils

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

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
    val nsDict = mapOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to SERVICE_NAME,
        kSecAttrAccount to key,
        kSecValueData to data,
        kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly
    ).toNSDictionary()

    // Bridge NSDictionary (ObjC) to CFDictionaryRef (C-Pointer)
    val query = CFBridgingRetain(nsDict) as? CFDictionaryRef
    try {
        SecItemAdd(query, null)
    } finally {
        if (query != null) CFRelease(query)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun loadFromKeychain(key: String): String? {
    memScoped {
        val nsDict = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to key,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne
        ).toNSDictionary()

        val query = CFBridgingRetain(nsDict) as? CFDictionaryRef
        try {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)

            if (status == errSecSuccess) {
                val resultRef = result.value
                if (resultRef != null) {
                    // Bridge back from C-Pointer to ObjC Object
                    val data = CFBridgingRelease(resultRef) as? NSData
                    return data?.toByteArray()?.decodeToString()
                }
            }
        } finally {
            if (query != null) CFRelease(query)
        }
        return null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun deleteFromKeychain(key: String) {
    val nsDict = mapOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to SERVICE_NAME,
        kSecAttrAccount to key
    ).toNSDictionary()

    val query = CFBridgingRetain(nsDict) as? CFDictionaryRef
    try {
        SecItemDelete(query)
    } finally {
        if (query != null) CFRelease(query)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    if (isEmpty()) return NSData()
    NSData.dataWithBytes(this@toNSData.refTo(0).getPointer(this), this@toNSData.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val len = this.length.toInt()
    if (len == 0) return ByteArray(0)
    return this.bytes?.readBytes(len) ?: ByteArray(0)
}

private fun Map<*, *>.toNSDictionary(): NSDictionary {
    return NSDictionary.dictionaryWithObjects(this.values.toList(), this.keys.toList()) as NSDictionary
}