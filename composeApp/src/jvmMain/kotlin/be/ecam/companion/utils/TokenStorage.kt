package be.ecam.companion.utils

import java.security.SecureRandom
import java.util.prefs.Preferences
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

private val prefs: Preferences = Preferences.userNodeForPackage(TokenStorageHelper::class.java)
private const val TOKEN_KEY = "jwt_token_encrypted"
private const val REFRESH_TOKEN_KEY = "refresh_token_encrypted"
private const val SALT_KEY = "encryption_salt"
private const val GCM_TAG_LENGTH = 128
private const val GCM_IV_LENGTH = 12

private object TokenStorageHelper

// Dérivation de clé basée sur une "passphrase" unique à la machine
private fun deriveKey(salt: ByteArray): SecretKeySpec {
    // Utiliser des infos machine comme "passphrase" (pas parfait mais mieux que rien)
    val machineId = System.getProperty("user.name") + System.getProperty("os.name") + "ClacOxygen"
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(machineId.toCharArray(), salt, 65536, 256)
    val key = factory.generateSecret(spec).encoded
    return SecretKeySpec(key, "AES")
}

private fun getOrCreateSalt(): ByteArray {
    val existing = prefs.get(SALT_KEY, null)
    return if (existing != null) {
        Base64.getDecoder().decode(existing)
    } else {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        prefs.put(SALT_KEY, Base64.getEncoder().encodeToString(salt))
        salt
    }
}

private fun encrypt(plainText: String): String {
    val salt = getOrCreateSalt()
    val key = deriveKey(salt)
    
    val iv = ByteArray(GCM_IV_LENGTH)
    SecureRandom().nextBytes(iv)
    
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
    
    val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    val combined = ByteArray(iv.size + encrypted.size)
    System.arraycopy(iv, 0, combined, 0, iv.size)
    System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
    
    return Base64.getEncoder().encodeToString(combined)
}

private fun decrypt(encryptedText: String): String? {
    return try {
        val salt = getOrCreateSalt()
        val key = deriveKey(salt)
        
        val combined = Base64.getDecoder().decode(encryptedText)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }
}

actual fun saveToken(token: String) {
    prefs.put(TOKEN_KEY, encrypt(token))
}

actual fun loadToken(): String? {
    val encrypted = prefs.get(TOKEN_KEY, null) ?: return null
    return decrypt(encrypted)
}

actual fun clearToken() {
    prefs.remove(TOKEN_KEY)
    prefs.remove(REFRESH_TOKEN_KEY)
}

actual fun saveRefreshToken(token: String) {
    prefs.put(REFRESH_TOKEN_KEY, encrypt(token))
}

actual fun loadRefreshToken(): String? {
    val encrypted = prefs.get(REFRESH_TOKEN_KEY, null) ?: return null
    return decrypt(encrypted)
}