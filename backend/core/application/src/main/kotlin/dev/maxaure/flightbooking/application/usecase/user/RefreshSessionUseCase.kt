package dev.maxaure.flightbooking.application.usecase.user

import dev.maxaure.flightbooking.application.port.`in`.RefreshSession
import dev.maxaure.flightbooking.application.port.`in`.RefreshedSession
import dev.maxaure.flightbooking.application.port.out.RefreshTokenGenerator
import dev.maxaure.flightbooking.application.port.out.RefreshTokenStore
import dev.maxaure.flightbooking.domain.model.user.RefreshToken
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Clock
import java.time.Duration
import java.util.UUID

class RefreshSessionUseCase(
    private val tokens: RefreshTokenStore,
    private val generator: RefreshTokenGenerator,
    private val clock: Clock,
    private val ttl: Duration,
) : RefreshSession {

    override fun start(userId: UserId): RefreshedSession {
        val now = clock.instant()
        val next = generator.generate()
        tokens.save(
            RefreshToken(
                id = UUID.randomUUID(),
                userId = userId.value,
                tokenHash = next.hash,
                familyId = UUID.randomUUID(),
                expiresAt = now.plus(ttl),
                createdAt = now,
                revokedAt = null,
            ),
        )
        return RefreshedSession(userId, next.raw)
    }

    override fun refresh(rawToken: String): RefreshedSession? {
        val now = clock.instant()
        val current = tokens.findByHash(generator.hash(rawToken)) ?: return null
        if (current.revokedAt != null) {
            tokens.revokeFamily(current.familyId, now)
            return null
        }
        if (!current.isUsableAt(now)) return null

        tokens.revoke(current.id, now)
        val next = generator.generate()
        tokens.save(
            RefreshToken(
                id = UUID.randomUUID(),
                userId = current.userId,
                tokenHash = next.hash,
                familyId = current.familyId,
                expiresAt = now.plus(ttl),
                createdAt = now,
                revokedAt = null,
            ),
        )
        return RefreshedSession(UserId.fromTrusted(current.userId), next.raw)
    }

    override fun revoke(rawToken: String) {
        val current = tokens.findByHash(generator.hash(rawToken)) ?: return
        if (current.revokedAt == null) tokens.revoke(current.id, clock.instant())
    }
}
