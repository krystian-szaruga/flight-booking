package dev.maxaure.flightbooking.persistence.repository

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class PassportCipher(encodedKey: String) {
    private val key = SecretKeySpec(Base64.getDecoder().decode(encodedKey), ALGORITHM)
    private val random = SecureRandom()

    init {
        require(key.encoded.size == KEY_BYTES) { "Passport encryption key must be 32 bytes" }
    }

    fun encrypt(passportNumber: String): String {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        val encrypted = cipher.doFinal(passportNumber.toByteArray(Charsets.UTF_8))
        return listOf(PREFIX, encoder.encodeToString(iv), encoder.encodeToString(encrypted)).joinToString(SEPARATOR)
    }

    fun decrypt(value: String): String {
        val parts = value.split(SEPARATOR)
        require(parts.size == 3 && parts[0] == PREFIX) { "Unsupported passport ciphertext format" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, decoder.decode(parts[1])))
        return String(cipher.doFinal(decoder.decode(parts[2])), Charsets.UTF_8)
    }

    fun last4(passportNumber: String): String = passportNumber.takeLast(LAST4_LENGTH)

    private companion object {
        const val ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BYTES = 32
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val LAST4_LENGTH = 4
        const val PREFIX = "v1"
        const val SEPARATOR = ":"
        val encoder: Base64.Encoder = Base64.getEncoder()
        val decoder: Base64.Decoder = Base64.getDecoder()
    }
}
