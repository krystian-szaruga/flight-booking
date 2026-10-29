package dev.maxaure.flightbooking.domain.model.user.verification

import java.time.Duration
import java.time.Instant

class ResendBackoff(private val ceiling: Duration) {

    init {
        require(!ceiling.isNegative && !ceiling.isZero) { "Backoff ceiling must be positive" }
    }

    fun delayFor(sendCount: Int): Duration {
        val seconds = STEPS_SECONDS.getOrElse(sendCount - 1) { ceiling.seconds }
        return Duration.ofSeconds(minOf(seconds, ceiling.seconds))
    }

    fun isCoolingDown(state: ResendState, now: Instant): Boolean = remainingCooldown(state, now).isPositive

    fun remainingCooldown(state: ResendState, now: Instant): Duration {
        val lastSentAt = state.lastSentAt ?: return Duration.ZERO
        val readyAt = lastSentAt.plus(delayFor(state.sendCount))
        return if (readyAt.isAfter(now)) Duration.between(now, readyAt) else Duration.ZERO
    }

    private companion object {
        val STEPS_SECONDS = listOf(60L, 5 * 60L, 15 * 60L)
    }
}
