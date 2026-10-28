package dev.maxaure.flightbooking.domain.model.user

import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import java.time.Instant
import java.util.UUID

@JvmInline
value class UserId(val value: UUID) {
    override fun toString(): String = value.toString()

    companion object {
        fun new(): UserId = UserId(UUID.randomUUID())

        fun of(raw: String): UserId =
            try {
                UserId(UUID.fromString(raw.trim()))
            } catch (e: IllegalArgumentException) {
                throw InvalidDomainValueException("User id must be a UUID, got '$raw'", e)
            }

        fun fromTrusted(value: UUID): UserId = UserId(value)
    }
}

@JvmInline
value class Email private constructor(val value: String) {
    override fun toString(): String = value

    companion object {
        private val PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        fun of(raw: String): Email {
            val normalised = raw.trim().lowercase()
            if (normalised.length > 254 || !PATTERN.matches(normalised)) {
                throw InvalidDomainValueException("Email must be a valid address, got '$raw'")
            }
            return Email(normalised)
        }

        fun fromTrusted(value: String): Email = Email(value)
    }
}

@JvmInline
value class PasswordHash private constructor(val value: String) {
    override fun toString(): String = "PasswordHash(***)"

    companion object {
        fun fromTrusted(value: String): PasswordHash {
            require(value.isNotBlank()) { "Password hash must not be blank" }
            return PasswordHash(value)
        }
    }
}

data class User(
    val id: UserId,
    val email: Email,
    val passwordHash: PasswordHash,
    val createdAt: Instant,
) {
    companion object {
        fun register(email: Email, passwordHash: PasswordHash, now: Instant, id: UserId = UserId.new()): User =
            User(id = id, email = email, passwordHash = passwordHash, createdAt = now)
    }
}
