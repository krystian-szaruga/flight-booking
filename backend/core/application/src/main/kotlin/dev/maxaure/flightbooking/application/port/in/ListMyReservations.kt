package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.user.UserId

interface ListMyReservations {
    fun list(userId: UserId): List<Reservation>
}
