package dev.maxaure.flightbooking.security

import dev.maxaure.flightbooking.application.port.out.LoginAttemptKey
import dev.maxaure.flightbooking.application.port.out.LoginAttemptLimiter
import dev.maxaure.flightbooking.domain.model.user.TooManyLoginAttempts
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

internal class InMemoryLoginAttemptLimiter(
    private val maxFailures: Int,
    private val window: Duration,
    private val clock: Clock,
) : LoginAttemptLimiter {

    private data class Attempts(val since: Instant, val count: Int)

    private val attempts = ConcurrentHashMap<LoginAttemptKey, Attempts>()

    init {
        require(maxFailures > 0) { "Failure threshold must be positive" }
        require(!window.isNegative && !window.isZero) { "Attempt window must be positive" }
    }

    override fun check(key: LoginAttemptKey) {
        val current = attempts[key] ?: return
        val now = clock.instant()
        if (isStale(current, now)) {
            attempts.remove(key, current)
            return
        }
        if (current.count >= maxFailures) throw TooManyLoginAttempts(window - Duration.between(current.since, now))
    }

    override fun recordFailure(key: LoginAttemptKey) {
        val now = clock.instant()
        attempts.compute(key) { _, current ->
            when {
                current == null || isStale(current, now) -> Attempts(now, 1)
                else -> Attempts(current.since, current.count + 1)
            }
        }
    }

    override fun clear(key: LoginAttemptKey) {
        attempts.remove(key)
    }

    private fun isStale(current: Attempts, now: Instant): Boolean =
        !current.since.plus(window).isAfter(now)
}
