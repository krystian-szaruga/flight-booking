package dev.maxaure.flightbooking.scheduler

import dev.maxaure.flightbooking.application.reservation.ReservationUseCase
import org.slf4j.LoggerFactory

internal class ReservationExpiryJob(
    private val reservations: ReservationUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sweep() {
        val released = reservations.releaseExpired()
        if (released.isNotEmpty()) {
            log.info("Released {} expired reservation(s): {}", released.size, released)
        }
    }
}
