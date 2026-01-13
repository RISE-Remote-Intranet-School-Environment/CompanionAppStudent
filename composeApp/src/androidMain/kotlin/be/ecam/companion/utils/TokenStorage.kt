package be.ecam.companion.utils

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val PREFS_NAME = "secure_token_prefs"
private const val ACCESS_TOKEN_KEY = "jwt_token"
private const val REFRESH_TOKEN_KEY = "refresh_token"
private const val KEYSTORE_ALIAS = "clacoxygen_token_key"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val GCM_TAG_LENGTH = 128

private var appContext: Context? = null

fun initTokenStorage(context: Context) {
    appContext = context.applicationContext
    ensureKeyExists()
}

private fun getPrefs(): SharedPreferences? {
    return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

private fun ensureKeyExists() {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    
    if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val keySpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        
        keyGenerator.init(keySpec)
        keyGenerator.generateKey()
    }
}

private fun getSecretKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    return keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
}

private fun encrypt(plainText: String): String {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
    
    val iv = cipher.iv
    val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    
    // Combiner IV + données chiffrées
    val combined = ByteArray(iv.size + encryptedBytes.size)
    System.arraycopy(iv, 0, combined, 0, iv.size)
    System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
    
    return Base64.encodeToString(combined, Base64.NO_WRAP)
}

private fun decrypt(encryptedText: String): String? {
    return try {
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        
        // Extraire IV (12 bytes pour GCM)
        val iv = combined.copyOfRange(0, 12)
        val encryptedBytes = combined.copyOfRange(12, combined.size)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        
        String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
    } catch (e: Exception) {
        // Token corrompu ou clé invalide
        null
    }
}

actual fun saveToken(token: String) {
    getPrefs()?.edit()?.putString(ACCESS_TOKEN_KEY, encrypt(token))?.apply()
}

actual fun loadToken(): String? {
    val encrypted = getPrefs()?.getString(ACCESS_TOKEN_KEY, null) ?: return null
    return decrypt(encrypted)
}

actual fun clearToken() {
    getPrefs()?.edit()?.clear()?.apply()
}

actual fun saveRefreshToken(token: String) {
    getPrefs()?.edit()?.putString(REFRESH_TOKEN_KEY, encrypt(token))?.apply()
}

actual fun loadRefreshToken(): String? {
    val encrypted = getPrefs()?.getString(REFRESH_TOKEN_KEY, null) ?: return null
    return decrypt(encrypted)
}
