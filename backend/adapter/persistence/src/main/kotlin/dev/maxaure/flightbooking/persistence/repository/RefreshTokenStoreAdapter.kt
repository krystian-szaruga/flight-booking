package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.RefreshTokenStore
import dev.maxaure.flightbooking.domain.model.user.RefreshToken
import dev.maxaure.flightbooking.persistence.entity.RefreshTokenEntity
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.UUID

internal class RefreshTokenStoreAdapter(
    private val jpa: SpringRefreshTokenJpaRepository,
    private val transactions: TransactionTemplate,
) : RefreshTokenStore {

    override fun save(token: RefreshToken) {
        jpa.save(token.toEntity())
    }

    override fun findByHash(tokenHash: String): RefreshToken? =
        jpa.findByTokenHash(tokenHash)?.toDomain()

    override fun revoke(id: UUID, at: Instant) {
        transactions.executeWithoutResult { jpa.revoke(id, at) }
    }

    override fun revokeFamily(familyId: UUID, at: Instant) {
        transactions.executeWithoutResult { jpa.revokeFamily(familyId, at) }
    }

    override fun purgeExpired(before: Instant): Int =
        transactions.execute { jpa.deleteExpired(before) } ?: 0

    private fun RefreshToken.toEntity(): RefreshTokenEntity =
        RefreshTokenEntity(id, userId, tokenHash, familyId, expiresAt, createdAt, revokedAt)

    private fun RefreshTokenEntity.toDomain(): RefreshToken =
        RefreshToken(id, userId, tokenHash, familyId, expiresAt, createdAt, revokedAt)
}
