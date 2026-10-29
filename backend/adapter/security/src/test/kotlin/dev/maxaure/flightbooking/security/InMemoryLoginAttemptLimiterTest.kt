package dev.maxaure.flightbooking.security

import dev.maxaure.flightbooking.application.port.out.LoginAttemptKey
import dev.maxaure.flightbooking.domain.model.user.TooManyLoginAttempts
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InMemoryLoginAttemptLimiterTest {

    private val clock = object : Clock() {
        var now: Instant = Instant.parse("2026-01-01T10:00:00Z")
        override fun getZone(): ZoneOffset = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = now
    }

    private val limiter = InMemoryLoginAttemptLimiter(3, Duration.ofMinutes(15), clock)
    private val key = LoginAttemptKey("10.0.0.1", "anna@example.com")

    @Test
    fun `when failures stay under the threshold then attempts are still allowed`() {
        repeat(2) { limiter.recordFailure(key) }

        limiter.check(key)
    }

    @Test
    fun `when the threshold is reached then the attempt is refused with the remaining window`() {
        repeat(3) { limiter.recordFailure(key) }
        clock.now = clock.now.plus(Duration.ofMinutes(5))

        val failure = assertFailsWith<TooManyLoginAttempts> { limiter.check(key) }

        assertEquals(Duration.ofMinutes(10), failure.retryAfter)
    }

    @Test
    fun `when the window passes then earlier failures no longer count`() {
        repeat(3) { limiter.recordFailure(key) }
        clock.now = clock.now.plus(Duration.ofMinutes(16))

        limiter.check(key)
    }

    @Test
    fun `when a successful login clears the key then the counter starts over`() {
        repeat(3) { limiter.recordFailure(key) }
        limiter.clear(key)

        limiter.check(key)
        limiter.recordFailure(key)
        limiter.check(key)
    }

    @Test
    fun `when the same email comes from another address then its counter is separate`() {
        repeat(3) { limiter.recordFailure(key) }

        limiter.check(LoginAttemptKey("10.0.0.2", "anna@example.com"))
    }
}
