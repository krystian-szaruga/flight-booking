package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.RevokedTokenRepository
import dev.maxaure.flightbooking.domain.model.user.RevokeTokenCommand
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RevokeTokenUseCaseTest {

    private class InMemoryRevokedTokens : RevokedTokenRepository {
        val rows = mutableMapOf<UUID, Instant>()

        override fun revoke(tokenId: UUID, expiresAt: Instant) {
            rows.putIfAbsent(tokenId, expiresAt)
        }

        override fun isRevoked(tokenId: UUID): Boolean = rows.containsKey(tokenId)

        override fun purgeExpired(before: Instant): Int {
            val expired = rows.filterValues { !it.isAfter(before) }.keys
            expired.forEach(rows::remove)
            return expired.size
        }
    }

    private val revoked = InMemoryRevokedTokens()
    private val revoke = RevokeTokenUseCase(revoked)

    @Test
    fun `when a token is revoked then it stays revoked until it expires`() {
        val tokenId = UUID.randomUUID()
        val expiresAt = Instant.parse("2026-01-01T11:00:00Z")

        assertFalse(revoked.isRevoked(tokenId))

        revoke.revoke(RevokeTokenCommand(tokenId, expiresAt))

        assertTrue(revoked.isRevoked(tokenId))
        assertEquals(0, revoked.purgeExpired(Instant.parse("2026-01-01T10:59:00Z")))
        assertTrue(revoked.isRevoked(tokenId))
        assertEquals(1, revoked.purgeExpired(Instant.parse("2026-01-01T12:00:00Z")))
        assertFalse(revoked.isRevoked(tokenId))
    }

    @Test
    fun `when the same token is revoked twice then only one row is kept`() {
        val tokenId = UUID.randomUUID()

        revoke.revoke(RevokeTokenCommand(tokenId, Instant.parse("2026-01-01T11:00:00Z")))
        revoke.revoke(RevokeTokenCommand(tokenId, Instant.parse("2026-01-01T11:00:00Z")))

        assertEquals(1, revoked.rows.size)
    }
}
