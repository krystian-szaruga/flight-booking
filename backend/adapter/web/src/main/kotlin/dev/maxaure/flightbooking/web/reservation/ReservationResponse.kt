package dev.maxaure.flightbooking.web.reservation

import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import java.time.Instant

internal data class ReservationResponse(
    val reservationId: String,
    val flightId: String,
    val status: String,
    val expiresAt: Instant?,
    val seats: List<HeldSeatResponse>,
) {
    companion object {
        fun from(reservation: Reservation) = ReservationResponse(
            reservationId = reservation.id.toString(),
            flightId = reservation.flightId.toString(),
            status = reservation.status.name,
            expiresAt = reservation.expiresAt,
            seats = reservation.seats.map { HeldSeatResponse(it.seatNumber.value, it.firstName, it.lastName) },
        )
    }
}

internal data class HeldSeatResponse(
    val seat: String,
    val firstName: String,
    val lastName: String,
)
