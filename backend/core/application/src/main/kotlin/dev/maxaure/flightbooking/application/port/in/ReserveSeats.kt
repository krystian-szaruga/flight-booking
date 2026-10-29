package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.reservation.PassengerType
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.util.UUID

data class HoldSeatRequest(
    val seatNumber: SeatNumber,
    val passengerType: PassengerType,
    val firstName: String? = null,
    val lastName: String? = null,
)

interface ReserveSeats {

    fun hold(userId: UserId, flightId: UUID, seats: List<HoldSeatRequest>): Reservation

    fun cancel(userId: UserId, reservationId: UUID): Reservation

    fun cancelSeats(userId: UserId, reservationId: UUID, seats: List<SeatNumber>): Reservation

    fun releaseExpired(limit: Int = 200): List<UUID>
}
