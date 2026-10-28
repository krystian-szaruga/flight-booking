package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.reservation.PassengerSeat
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import java.util.UUID

interface ReserveSeats {

    fun hold(flightId: UUID, seats: List<PassengerSeat>): Reservation

    fun cancel(reservationId: UUID): Reservation

    fun releaseExpired(limit: Int = 200): List<UUID>
}
