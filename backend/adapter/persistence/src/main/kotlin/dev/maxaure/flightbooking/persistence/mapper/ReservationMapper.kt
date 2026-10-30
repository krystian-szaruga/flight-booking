package dev.maxaure.flightbooking.persistence.mapper

import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.reservation.PassengerSeat
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.reservation.ReservationStatus
import dev.maxaure.flightbooking.persistence.entity.ReservationEntity
import dev.maxaure.flightbooking.persistence.entity.ReservationSeatEntity
import dev.maxaure.flightbooking.persistence.repository.PassportCipher
import java.util.UUID

internal object ReservationMapper {

    fun toDomain(entity: ReservationEntity, seats: List<PassengerSeat>): Reservation = Reservation(
        id = entity.id,
        flightId = entity.flightId,
        userId = entity.userId,
        status = ReservationStatus.valueOf(entity.status),
        expiresAt = entity.expiresAt,
        createdAt = entity.createdAt,
        seats = seats,
    )

    fun toEntity(reservation: Reservation): ReservationEntity = ReservationEntity(
        id = reservation.id,
        flightId = reservation.flightId,
        userId = reservation.userId,
        status = reservation.status.name,
        expiresAt = reservation.expiresAt,
        createdAt = reservation.createdAt,
    )

    fun seatToDomain(seat: ReservationSeatEntity, seatNumber: SeatNumber): PassengerSeat =
        PassengerSeat(
            seatNumber = seatNumber,
            firstName = seat.firstName,
            lastName = seat.lastName,
            passportNumber = null,
            passportNumberLast4 = seat.passportNumberLast4,
        )

    fun seatToEntity(
        reservationId: UUID,
        flightSeatId: UUID,
        seat: PassengerSeat,
        passportCipher: PassportCipher,
    ): ReservationSeatEntity =
        ReservationSeatEntity(
            id = UUID.randomUUID(),
            reservationId = reservationId,
            flightSeatId = flightSeatId,
            firstName = seat.firstName,
            lastName = seat.lastName,
            passportNumberCiphertext = seat.passportNumber?.let(passportCipher::encrypt),
            passportNumberLast4 = seat.passportNumber?.let(passportCipher::last4),
        )
}
