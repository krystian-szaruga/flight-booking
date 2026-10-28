package dev.maxaure.flightbooking.application.port.out

import java.time.Instant
import java.util.UUID

interface RevokedTokenRepository {
    fun revoke(tokenId: UUID, expiresAt: Instant)

    fun isRevoked(tokenId: UUID): Boolean

    fun purgeExpired(before: Instant): Int
}
