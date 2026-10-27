package dev.maxaure.flightbooking.application.reservation

import dev.maxaure.flightbooking.domain.ConflictException
import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.catalogue.model.SeatNumber
import dev.maxaure.flightbooking.domain.reservation.model.PassengerSeat
import dev.maxaure.flightbooking.domain.reservation.model.Reservation
import dev.maxaure.flightbooking.domain.reservation.model.ReservationStatus
import java.time.Clock
import java.util.UUID

class ReservationUseCase(
    private val reservations: ReservationRepository,
    private val clock: Clock,
) {

    fun hold(flightId: UUID, seats: List<PassengerSeat>): Reservation {
        if (seats.isEmpty()) {
            throw InvalidDomainValueException("A hold must include at least one seat")
        }
        val candidate = Reservation.hold(flightId, seats, clock.instant())
        return when (val outcome = reservations.holdSeats(candidate)) {
            is HoldOutcome.Held -> outcome.reservation
            is HoldOutcome.SeatUnavailable -> throw ConflictException(
                "Seats ${outcome.seatNumbers.joinToString()} are no longer available on flight $flightId",
            )
        }
    }

    fun cancel(reservationId: UUID): Reservation =
        reservations.releaseSeats(reservationId)
            ?: throw NotFoundException("Reservation $reservationId does not exist")

    fun releaseExpired(limit: Int = EXPIRY_BATCH): List<UUID> {
        val now = clock.instant()
        return reservations.findExpired(now, limit)
            .filter { reservations.releaseSeats(it.id)?.status == ReservationStatus.EXPIRED }
            .map { it.id }
    }

    fun seatNumbers(seats: List<PassengerSeat>): List<SeatNumber> = seats.map { it.seatNumber }

    private companion object {
        const val EXPIRY_BATCH = 200
    }
}
