package dev.maxaure.flightbooking.domain.model.reservation

import dev.maxaure.flightbooking.domain.ConflictException
import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class ReservationStatus {
    PENDING,
    CONFIRMED,
    EXPIRED,
    CANCELLED,
}

data class PassengerSeat(val seatNumber: SeatNumber, val passengerName: String)

data class Reservation(
    val id: UUID,
    val flightId: UUID,
    val userId: UUID?,
    val status: ReservationStatus,
    val expiresAt: Instant?,
    val createdAt: Instant,
    val seats: List<PassengerSeat>,
) {
    init {
        require(seats.isNotEmpty()) { "A reservation must hold at least one seat" }
        require(seats.map { it.seatNumber }.toSet().size == seats.size) {
            "A seat may only be held once per reservation"
        }
        seats.forEach {
            if (it.passengerName.isBlank()) throw InvalidDomainValueException("Passenger name must not be blank")
        }
        val holdsSeats = status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED
        require(holdsSeats == (expiresAt != null)) { "Only a pending reservation carries a hold expiry" }
    }

    fun expire(at: Instant): Reservation {
        if (status != ReservationStatus.PENDING) {
            throw ConflictException("Reservation $id is ${status.name.lowercase()} and cannot expire")
        }
        require(!at.isBefore(createdAt)) { "A reservation cannot expire before it was created" }
        return copy(status = ReservationStatus.EXPIRED, expiresAt = null)
    }

    fun cancel(): Reservation {
        if (status != ReservationStatus.PENDING) {
            throw ConflictException("Reservation $id is ${status.name.lowercase()} and cannot be cancelled")
        }
        return copy(status = ReservationStatus.CANCELLED, expiresAt = null)
    }

    companion object {
        val HOLD_DURATION: Duration = Duration.ofMinutes(15)

        fun hold(
            flightId: UUID,
            seats: List<PassengerSeat>,
            now: Instant,
            userId: UUID? = null,
            id: UUID = UUID.randomUUID(),
            holdDuration: Duration = HOLD_DURATION,
        ): Reservation {
            require(!holdDuration.isNegative && !holdDuration.isZero) { "Hold duration must be positive" }
            return Reservation(
                id = id,
                flightId = flightId,
                userId = userId,
                status = ReservationStatus.PENDING,
                expiresAt = now.plus(holdDuration),
                createdAt = now,
                seats = seats,
            )
        }
    }
}
