package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.ConflictException
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import java.time.Instant
import java.util.UUID

interface ReservationRepository {

    fun findById(id: UUID): Reservation?

    fun save(reservation: Reservation): Reservation

    fun holdSeats(reservation: Reservation): HoldOutcome

    fun releaseSeats(reservationId: UUID): Reservation?

    fun findExpired(at: Instant, limit: Int): List<Reservation>

    fun findByUserId(userId: UUID): List<Reservation>
}

sealed interface HoldOutcome {
    data class Held(val reservation: Reservation) : HoldOutcome

    data class SeatUnavailable(val seatNumbers: List<String>) : HoldOutcome {
        fun toConflict(flightId: UUID): ConflictException = ConflictException(
            "Seats ${seatNumbers.joinToString()} are no longer available on flight $flightId",
        )
    }
}
