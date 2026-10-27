package dev.maxaure.flightbooking.persistence.mapper

import dev.maxaure.flightbooking.domain.catalogue.model.SeatNumber
import dev.maxaure.flightbooking.domain.reservation.model.PassengerSeat
import dev.maxaure.flightbooking.domain.reservation.model.Reservation
import dev.maxaure.flightbooking.domain.reservation.model.ReservationStatus
import dev.maxaure.flightbooking.persistence.entity.ReservationEntity
import dev.maxaure.flightbooking.persistence.entity.ReservationSeatEntity
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
        PassengerSeat(seatNumber = seatNumber, passengerName = seat.passengerName)

    fun seatToEntity(reservationId: UUID, flightSeatId: UUID, seat: PassengerSeat): ReservationSeatEntity =
        ReservationSeatEntity(
            id = UUID.randomUUID(),
            reservationId = reservationId,
            flightSeatId = flightSeatId,
            passengerName = seat.passengerName,
        )
}
