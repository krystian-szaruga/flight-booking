package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.RevokedTokenRepository
import dev.maxaure.flightbooking.persistence.entity.RevokedTokenEntity
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.UUID

internal class RevokedTokenRepositoryAdapter(
    private val jpa: SpringRevokedTokenJpaRepository,
    private val transactions: TransactionTemplate,
) : RevokedTokenRepository {

    override fun revoke(tokenId: UUID, expiresAt: Instant) {
        if (!jpa.existsById(tokenId)) jpa.save(RevokedTokenEntity(tokenId, expiresAt))
    }

    override fun isRevoked(tokenId: UUID): Boolean = jpa.existsById(tokenId)

    override fun purgeExpired(before: Instant): Int =
        transactions.execute { jpa.deleteExpired(before) }
}
