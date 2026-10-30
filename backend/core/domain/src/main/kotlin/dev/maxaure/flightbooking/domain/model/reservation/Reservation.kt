package dev.maxaure.flightbooking.domain.model.reservation

import dev.maxaure.flightbooking.domain.ConflictException
import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class PassengerType {
    SELF,
    OTHER,
}

data class PassengerSeat(
    val seatNumber: SeatNumber,
    val firstName: String,
    val lastName: String,
    val passportNumber: String? = null,
    val passportNumberLast4: String? = null,
) {
    val fullName: String
        get() = "$firstName $lastName"

    init {
        if (firstName.isBlank() || lastName.isBlank()) {
            throw InvalidDomainValueException("Passenger first and last name must both be provided")
        }
        if (passportNumber != null && passportNumber.isBlank()) {
            throw InvalidDomainValueException("Passport number must not be blank when provided")
        }
    }

    override fun toString(): String =
        "PassengerSeat(seatNumber=$seatNumber, firstName=$firstName, lastName=$lastName, passportNumber=${passportNumberLast4?.let { "••••$it" } ?: "none"})"
}

enum class ReservationStatus {
    PENDING,
    CONFIRMED,
    EXPIRED,
    CANCELLED,
}

data class Reservation(
    val id: UUID,
    val flightId: UUID,
    val userId: UUID,
    val status: ReservationStatus,
    val expiresAt: Instant?,
    val createdAt: Instant,
    val seats: List<PassengerSeat>,
) {
    init {
        val holdsSeats = status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED
        require(!holdsSeats || seats.isNotEmpty()) { "A reservation that holds seats must have at least one seat" }
        require(seats.map { it.seatNumber }.toSet().size == seats.size) {
            "A seat may only be held once per reservation"
        }
        seats.forEach {
            if (it.fullName.isBlank()) throw InvalidDomainValueException("Passenger name must not be blank")
        }
        require((status == ReservationStatus.PENDING) == (expiresAt != null)) {
            "Only a pending reservation carries a hold expiry"
        }
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

    fun pay(now: Instant): Reservation {
        if (status != ReservationStatus.PENDING) {
            throw ConflictException("Reservation $id is ${status.name.lowercase()} and cannot be paid")
        }
        val holdEndsAt = requireNotNull(expiresAt) { "A pending reservation must carry a hold expiry" }
        if (!now.isBefore(holdEndsAt)) {
            throw ConflictException("Reservation $id hold has expired and cannot be paid")
        }
        return copy(status = ReservationStatus.CONFIRMED, expiresAt = null)
    }

    companion object {
        val HOLD_DURATION: Duration = Duration.ofMinutes(15)

        fun hold(
            flightId: UUID,
            seats: List<PassengerSeat>,
            now: Instant,
            userId: UUID,
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
