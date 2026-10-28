package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.ReserveSeats
import dev.maxaure.flightbooking.application.port.out.HoldOutcome
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.reservation.PassengerSeat
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.reservation.ReservationStatus
import java.time.Clock
import java.util.UUID

class ReservationUseCase(
    private val reservations: ReservationRepository,
    private val clock: Clock,
) : ReserveSeats {

    override fun hold(flightId: UUID, seats: List<PassengerSeat>): Reservation {
        val candidate = Reservation.hold(flightId, seats, clock.instant())
        return when (val outcome = reservations.holdSeats(candidate)) {
            is HoldOutcome.Held -> outcome.reservation
            is HoldOutcome.SeatUnavailable -> throw outcome.toConflict(flightId)
        }
    }

    override fun cancel(reservationId: UUID): Reservation =
        reservations.releaseSeats(reservationId)
            ?: throw NotFoundException("Reservation $reservationId does not exist")

    override fun releaseExpired(limit: Int): List<UUID> =
        reservations.findExpired(clock.instant(), limit)
            .mapNotNull { reservations.releaseSeats(it.id) }
            .filter { it.status == ReservationStatus.EXPIRED }
            .map { it.id }
}
