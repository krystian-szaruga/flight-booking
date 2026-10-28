package dev.maxaure.flightbooking.scheduler

import dev.maxaure.flightbooking.application.port.`in`.ReserveSeats
import org.slf4j.LoggerFactory

internal class ReservationExpiryJob(
    private val reservations: ReserveSeats,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sweep() {
        val released = reservations.releaseExpired()
        if (released.isNotEmpty()) {
            log.info("Released {} expired reservation(s): {}", released.size, released)
        }
    }
}
