package dev.maxaure.flightbooking.domain.model.user

import java.time.Instant
import java.util.UUID

data class RefreshToken(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val familyId: UUID,
    val expiresAt: Instant,
    val createdAt: Instant,
    val revokedAt: Instant?,
) {
    fun isUsableAt(now: Instant): Boolean = revokedAt == null && expiresAt.isAfter(now)
}
