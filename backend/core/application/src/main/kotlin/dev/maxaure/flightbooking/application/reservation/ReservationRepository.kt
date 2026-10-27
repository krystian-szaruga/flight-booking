package dev.maxaure.flightbooking.application.reservation

import dev.maxaure.flightbooking.domain.reservation.model.Reservation
import java.time.Instant
import java.util.UUID

interface ReservationRepository {

    fun findById(id: UUID): Reservation?

    fun save(reservation: Reservation): Reservation

    fun holdSeats(reservation: Reservation): HoldOutcome

    fun releaseSeats(reservationId: UUID): Reservation?

    fun findExpired(at: Instant, limit: Int): List<Reservation>
}

sealed interface HoldOutcome {
    data class Held(val reservation: Reservation) : HoldOutcome

    data class SeatUnavailable(val seatNumbers: List<String>) : HoldOutcome
}
