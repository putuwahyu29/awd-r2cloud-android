package com.awd.r2cloud.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionManager @Inject constructor() {
    private val algorithm = "AES/GCM/NoPadding"
    private val tagLength = 128
    private val ivLength = 12

    fun encrypt(data: ByteArray, keyString: String): ByteArray {
        val key = deriveKey(keyString)
        val cipher = Cipher.getInstance(algorithm)
        val iv = ByteArray(ivLength).apply { SecureRandom().nextBytes(this) }
        val gcmSpec = GCMParameterSpec(tagLength, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)
        val ciphertext = cipher.doFinal(data)
        
        // Return IV + Ciphertext
        return iv + ciphertext
    }

    fun decrypt(encryptedData: ByteArray, keyString: String): ByteArray {
        val key = deriveKey(keyString)
        val iv = encryptedData.sliceArray(0 until ivLength)
        val ciphertext = encryptedData.sliceArray(ivLength until encryptedData.size)
        
        val cipher = Cipher.getInstance(algorithm)
        val gcmSpec = GCMParameterSpec(tagLength, iv)
        
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(keyString: String): SecretKeySpec {
        // Basic key derivation for simplicity, in production use PBKDF2
        val keyBytes = keyString.padEnd(32, ' ').take(32).toByteArray()
        return SecretKeySpec(keyBytes, "AES")
    }
}
