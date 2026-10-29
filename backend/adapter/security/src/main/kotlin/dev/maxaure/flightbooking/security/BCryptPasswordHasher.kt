package dev.maxaure.flightbooking.security

import dev.maxaure.flightbooking.application.port.out.PasswordHasher
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

internal class BCryptPasswordHasher(
    private val encoder: BCryptPasswordEncoder,
) : PasswordHasher {

    override fun hash(rawPassword: String): PasswordHash = PasswordHash.fromTrusted(encoder.encode(rawPassword)!!)

    override fun matches(rawPassword: String, stored: PasswordHash): Boolean =
        encoder.matches(rawPassword, stored.value)
}
