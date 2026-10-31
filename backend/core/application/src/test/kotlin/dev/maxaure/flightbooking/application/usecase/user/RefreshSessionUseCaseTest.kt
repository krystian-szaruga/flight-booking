package dev.maxaure.flightbooking.application.usecase.user

import dev.maxaure.flightbooking.application.port.out.GeneratedRefreshToken
import dev.maxaure.flightbooking.application.port.out.RefreshTokenGenerator
import dev.maxaure.flightbooking.application.port.out.RefreshTokenStore
import dev.maxaure.flightbooking.domain.model.user.RefreshToken
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RefreshSessionUseCaseTest {

    private class InMemoryRefreshTokens : RefreshTokenStore {
        val rows = mutableMapOf<UUID, RefreshToken>()

        override fun save(token: RefreshToken) {
            rows[token.id] = token
        }

        override fun findByHash(tokenHash: String): RefreshToken? =
            rows.values.firstOrNull { it.tokenHash == tokenHash }

        override fun revoke(id: UUID, at: Instant) {
            rows[id]?.let { rows[id] = it.copy(revokedAt = at) }
        }

        override fun revokeFamily(familyId: UUID, at: Instant) {
            rows.replaceAll { _, token ->
                if (token.familyId == familyId && token.revokedAt == null) token.copy(revokedAt = at) else token
            }
        }

        override fun purgeExpired(before: Instant): Int {
            val expired = rows.values.filter { !it.expiresAt.isAfter(before) }.map { it.id }
            expired.forEach(rows::remove)
            return expired.size
        }
    }

    private class SequentialRefreshTokens : RefreshTokenGenerator {
        var issued = 0

        override fun generate(): GeneratedRefreshToken {
            issued++
            return GeneratedRefreshToken("raw-$issued", hash("raw-$issued"))
        }

        override fun hash(rawToken: String): String = "hash-$rawToken"
    }

    private val now = Instant.parse("2026-01-01T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val store = InMemoryRefreshTokens()
    private val generator = SequentialRefreshTokens()
    private val sessions = RefreshSessionUseCase(store, generator, clock, Duration.ofDays(7))
    private val userId = UserId.fromTrusted(UUID.randomUUID())

    @Test
    fun `when a session starts then only the token hash is stored`() {
        val started = sessions.start(userId)

        val stored = store.findByHash(generator.hash(started.rawToken))
        assertNotNull(stored)
        assertEquals(userId.value, stored.userId)
        assertEquals("hash-${started.rawToken}", stored.tokenHash)
        assertEquals(now.plus(Duration.ofDays(7)), stored.expiresAt)
        assertNull(stored.revokedAt)
    }

    @Test
    fun `when a valid token is refreshed then it is rotated inside the same family`() {
        val started = sessions.start(userId)
        val original = assertNotNull(store.findByHash(generator.hash(started.rawToken)))

        val refreshed = assertNotNull(sessions.refresh(started.rawToken))

        val rotated = assertNotNull(store.findByHash(generator.hash(refreshed.rawToken)))
        assertEquals(original.familyId, rotated.familyId)
        assertEquals(userId, refreshed.userId)
        assertNotNull(store.findByHash(original.tokenHash)?.revokedAt)
        assertNull(rotated.revokedAt)
    }

    @Test
    fun `when a rotated token is used again then the whole family is revoked`() {
        val started = sessions.start(userId)
        val original = assertNotNull(store.findByHash(generator.hash(started.rawToken)))
        val refreshed = assertNotNull(sessions.refresh(started.rawToken))

        assertNull(sessions.refresh(started.rawToken))

        assertTrue(store.rows.values.filter { it.familyId == original.familyId }.all { it.revokedAt != null })
        assertNull(sessions.refresh(refreshed.rawToken))
    }

    @Test
    fun `when a token is past its expiry then refreshing it is rejected`() {
        val started = sessions.start(userId)
        val later = Clock.fixed(now.plus(Duration.ofDays(8)), ZoneOffset.UTC)
        val expired = RefreshSessionUseCase(store, generator, later, Duration.ofDays(7))

        assertNull(expired.refresh(started.rawToken))
    }

    @Test
    fun `when a session is revoked then its token can no longer be refreshed`() {
        val started = sessions.start(userId)

        sessions.revoke(started.rawToken)

        assertNull(sessions.refresh(started.rawToken))
    }
}
