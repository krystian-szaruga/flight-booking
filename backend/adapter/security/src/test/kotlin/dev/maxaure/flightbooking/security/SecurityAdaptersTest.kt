package dev.maxaure.flightbooking.security

import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecurityAdaptersTest {

    private val secret = "0123456789abcdef0123456789abcdef"
    private val clock = object : Clock() {
        var now: Instant = Instant.parse("2026-01-01T10:00:00Z")
        override fun getZone(): ZoneOffset = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId): Clock = this
        override fun instant(): Instant = now
    }

    private val issuer = JwtAccessTokenIssuer(secret, Duration.ofMinutes(15), clock)

    @Test
    fun `when bcrypt hashes a password then it verifies only the original`() {
        val hasher = BCryptPasswordHasher(org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder())

        val hash = hasher.hash("s3cret")

        assertTrue(hash.value.startsWith("\$2"))
        assertTrue(hasher.matches("s3cret", hash))
        assertFalse(hasher.matches("wrong", hash))
        assertFalse(hash.value.contains("s3cret"))
    }

    @Test
    fun `when a token round-trips then it yields the same user id`() {
        val userId = UserId.new()

        val token = issuer.issue(userId)
        val parsed = issuer.parse(token)

        assertEquals(userId, parsed?.userId)
        assertEquals(clock.now.plus(Duration.ofMinutes(15)), parsed?.expiresAt)
        assertNull(issuer.parse("not-a-token"))
    }

    @Test
    fun `when two tokens are issued then each carries a distinct id`() {
        val userId = UserId.new()

        val first = issuer.parse(issuer.issue(userId))
        val second = issuer.parse(issuer.issue(userId))

        assertEquals(userId, first?.userId)
        assertTrue(first?.id != null && first.id != second?.id)
    }

    @Test
    fun `when a token is expired then parsing rejects it`() {
        val token = issuer.issue(UserId.new())

        clock.now = clock.now.plus(Duration.ofMinutes(16))

        assertNull(issuer.parse(token))
    }

    @Test
    fun `when a token is tampered or signed with another secret then parsing rejects it`() {
        val token = issuer.issue(UserId.new())
        val foreign = JwtAccessTokenIssuer("fedcba9876543210fedcba9876543210", Duration.ofMinutes(15), clock)

        assertNull(foreign.parse(token))
        assertNull(issuer.parse(token.dropLast(3) + "abc"))
    }

    @Test
    fun `when a password hash is printed then it is masked`() {
        val hash = PasswordHash.fromTrusted("\$2a\$10\$abcdefghijklmnopqrstuv")

        assertEquals("PasswordHash(***)", hash.toString())
    }
}
