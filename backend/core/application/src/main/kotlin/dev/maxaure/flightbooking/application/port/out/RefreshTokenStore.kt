package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.user.RefreshToken
import java.time.Instant
import java.util.UUID

interface RefreshTokenStore {
    fun save(token: RefreshToken)

    fun findByHash(tokenHash: String): RefreshToken?

    fun revoke(id: UUID, at: Instant)

    fun revokeFamily(familyId: UUID, at: Instant)

    fun purgeExpired(before: Instant): Int
}
