package dev.maxaure.flightbooking.domain.model.user.verification

import java.time.Instant

data class ResendState(
    val lastSentAt: Instant?,
    val sendCount: Int,
)
