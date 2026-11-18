package com.example.crypto

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val iv: String,
    val ciphertext: String
)

class CryptoManager(context: Context) {

    private val prefs = context.getSharedPreferences("aegis_secure_vault", Context.MODE_PRIVATE)
    private val keyAlias = "aes_master_key_v1"
    private val secretKey: SecretKey

    init {
        secretKey = getOrCreateSecretKey()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existingKeyBase64 = prefs.getString(keyAlias, null)
        return if (existingKeyBase64 != null) {
            val keyBytes = Base64.decode(existingKeyBase64, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256, SecureRandom())
            val newKey = keyGen.generateKey()
            val encodedKey = Base64.encodeToString(newKey.encoded, Base64.DEFAULT)
            prefs.edit().putString(keyAlias, encodedKey).apply()
            newKey
        }
    }

    /**
     * Encrypts plaintext using AES-256-GCM authenticated encryption.
     */
    fun encrypt(plainText: String): EncryptedPayload {
        if (plainText.isEmpty()) return EncryptedPayload("", "")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12) // 96-bit IV recommended for GCM
        SecureRandom().nextBytes(iv)

        val parameterSpec = GCMParameterSpec(128, iv) // 128-bit authentication tag
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        )
    }

    /**
     * Decrypts ciphertext using AES-256-GCM.
     */
    fun decrypt(iv: String, ciphertext: String): String {
        if (ciphertext.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val ivBytes = Base64.decode(iv, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(ciphertext, Base64.NO_WRAP)

            val parameterSpec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "[Decryption Error: Tampered or Invalid Key]"
        }
    }

    fun getAlgorithm(): String = "AES-256-GCM (256-bit Key, 128-bit Auth Tag)"
}
