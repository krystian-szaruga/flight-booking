package dev.maxaure.flightbooking.security

import dev.maxaure.flightbooking.application.port.out.GeneratedRefreshToken
import dev.maxaure.flightbooking.application.port.out.RefreshTokenGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

internal class SecureRandomRefreshTokenGenerator : RefreshTokenGenerator {

    private val random = SecureRandom()

    override fun generate(): GeneratedRefreshToken {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        val raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return GeneratedRefreshToken(raw, hash(raw))
    }

    override fun hash(rawToken: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(rawToken.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val TOKEN_BYTES = 32
    }
}
