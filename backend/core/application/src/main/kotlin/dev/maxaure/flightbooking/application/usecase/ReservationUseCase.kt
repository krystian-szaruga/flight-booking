package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.HoldSeatRequest
import dev.maxaure.flightbooking.application.port.`in`.ReserveSeats
import dev.maxaure.flightbooking.application.port.out.HoldOutcome
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.domain.ConflictException
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.reservation.PassengerSeat
import dev.maxaure.flightbooking.domain.model.reservation.PassengerType
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.reservation.ReservationStatus
import dev.maxaure.flightbooking.domain.model.user.EmailNotVerified
import dev.maxaure.flightbooking.domain.model.user.ProfileIncomplete
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Clock
import java.util.UUID

class ReservationUseCase(
    private val reservations: ReservationRepository,
    private val users: UserRepository,
    private val clock: Clock,
) : ReserveSeats {

    override fun hold(userId: UserId, flightId: UUID, seats: List<HoldSeatRequest>): Reservation {
        val user = users.findById(userId) ?: throw NotFoundException("User $userId does not exist")
        if (!user.isVerified) throw EmailNotVerified()
        val passengers = seats.map { seat -> resolvePassenger(user, seat) }
        val candidate = Reservation.hold(flightId, passengers, clock.instant(), userId = userId.value)
        return when (val outcome = reservations.holdSeats(candidate)) {
            is HoldOutcome.Held -> outcome.reservation
            is HoldOutcome.SeatUnavailable -> throw outcome.toConflict(flightId)
        }
    }

    private fun resolvePassenger(user: User, seat: HoldSeatRequest): PassengerSeat = when (seat.passengerType) {
        PassengerType.SELF -> {
            val first = user.firstName
            val last = user.lastName
            if (first.isNullOrBlank() || last.isNullOrBlank()) throw ProfileIncomplete()
            PassengerSeat(seat.seatNumber, first, last)
        }

        PassengerType.OTHER -> PassengerSeat(
            seat.seatNumber,
            seat.firstName?.trim().orEmpty(),
            seat.lastName?.trim().orEmpty(),
        )
    }

    override fun cancel(userId: UserId, reservationId: UUID): Reservation {
        val reservation = ownedOrMissing(userId, reservationId)
        return reservations.releaseSeats(reservationId)
            ?: throw NotFoundException("Reservation $reservationId does not exist")
    }

    override fun cancelSeats(userId: UserId, reservationId: UUID, seats: List<SeatNumber>): Reservation {
        val reservation = ownedOrMissing(userId, reservationId)
        val requested = seats.map { it.value }.toSet()
        val held = reservation.seats.map { it.seatNumber.value }.toSet()
        val unknown = requested - held
        if (unknown.isNotEmpty()) {
            throw NotFoundException("Seats ${unknown.joinToString()} are not part of reservation $reservationId")
        }
        if (requested.size == held.size) {
            return reservations.releaseSeats(reservationId)
                ?: throw NotFoundException("Reservation $reservationId does not exist")
        }
        return reservations.releaseSeatSubset(reservationId, requested.toList())
            ?: throw NotFoundException("Reservation $reservationId does not exist")
    }

    private fun ownedOrMissing(userId: UserId, reservationId: UUID): Reservation {
        val reservation = reservations.findById(reservationId)
            ?: throw NotFoundException("Reservation $reservationId does not exist")
        if (reservation.userId != userId.value) throw NotFoundException("Reservation $reservationId does not exist")
        return reservation
    }

    override fun releaseExpired(limit: Int): List<UUID> =
        reservations.findExpired(clock.instant(), limit)
            .mapNotNull { reservations.releaseSeats(it.id) }
            .filter { it.status == ReservationStatus.EXPIRED }
            .map { it.id }
}
