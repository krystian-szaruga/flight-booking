package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.ReserveSeats
import dev.maxaure.flightbooking.application.port.out.HoldOutcome
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.domain.ConflictException
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.reservation.PassengerSeat
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.reservation.ReservationStatus
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Clock
import java.util.UUID

class ReservationUseCase(
    private val reservations: ReservationRepository,
    private val users: UserRepository,
    private val clock: Clock,
) : ReserveSeats {

    override fun hold(userId: UserId, flightId: UUID, seats: List<PassengerSeat>): Reservation {
        if (users.findById(userId) == null) throw NotFoundException("User $userId does not exist")
        val candidate = Reservation.hold(flightId, seats, clock.instant(), userId = userId.value)
        return when (val outcome = reservations.holdSeats(candidate)) {
            is HoldOutcome.Held -> outcome.reservation
            is HoldOutcome.SeatUnavailable -> throw outcome.toConflict(flightId)
        }
    }

    override fun cancel(userId: UserId, reservationId: UUID): Reservation {
        val reservation = reservations.findById(reservationId)
            ?: throw NotFoundException("Reservation $reservationId does not exist")
        if (reservation.userId != userId.value) throw NotFoundException("Reservation $reservationId does not exist")
        return reservations.releaseSeats(reservationId)
            ?: throw NotFoundException("Reservation $reservationId does not exist")
    }

    override fun releaseExpired(limit: Int): List<UUID> =
        reservations.findExpired(clock.instant(), limit)
            .mapNotNull { reservations.releaseSeats(it.id) }
            .filter { it.status == ReservationStatus.EXPIRED }
            .map { it.id }
}
