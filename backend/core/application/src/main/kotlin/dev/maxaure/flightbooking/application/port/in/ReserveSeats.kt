package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.reservation.PassengerSeat
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.util.UUID

interface ReserveSeats {

    fun hold(userId: UserId, flightId: UUID, seats: List<PassengerSeat>): Reservation

    fun cancel(userId: UserId, reservationId: UUID): Reservation

    fun releaseExpired(limit: Int = 200): List<UUID>
}
