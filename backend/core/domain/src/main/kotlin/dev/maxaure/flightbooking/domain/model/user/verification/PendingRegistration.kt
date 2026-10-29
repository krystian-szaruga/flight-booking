package dev.maxaure.flightbooking.domain.model.user.verification

import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Instant

data class PendingRegistration(
    val email: Email,
    val passwordHash: PasswordHash,
    val codeHash: String,
    val expiresAt: Instant,
    val attempts: Int,
    val createdAt: Instant,
    val sentAt: Instant,
    val sendCount: Int,
) {
    fun isAcceptingCodes(now: Instant): Boolean = !isExpiredAt(now) && attempts < MAX_ATTEMPTS

    fun isExpiredAt(now: Instant): Boolean = !expiresAt.isAfter(now)

    fun replaceCode(codeHash: String, expiresAt: Instant, now: Instant): PendingRegistration =
        copy(codeHash = codeHash, expiresAt = expiresAt, attempts = 0, sentAt = now, sendCount = sendCount + 1)

    fun recordAttempt(): PendingRegistration = copy(attempts = attempts + 1)

    fun toUser(now: Instant, id: UserId = UserId.new()): User =
        User(id = id, email = email, passwordHash = passwordHash, createdAt = now, emailVerifiedAt = now)

    companion object {
        const val MAX_ATTEMPTS = 5

        fun start(
            email: Email,
            passwordHash: PasswordHash,
            codeHash: String,
            expiresAt: Instant,
            now: Instant,
        ): PendingRegistration = PendingRegistration(
            email = email,
            passwordHash = passwordHash,
            codeHash = codeHash,
            expiresAt = expiresAt,
            attempts = 0,
            createdAt = now,
            sentAt = now,
            sendCount = 1,
        )

        fun resendStateOf(pending: PendingRegistration): ResendState =
            ResendState(lastSentAt = pending.sentAt, sendCount = pending.sendCount)
    }
}
