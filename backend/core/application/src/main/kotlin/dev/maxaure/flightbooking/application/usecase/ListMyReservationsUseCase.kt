package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.ListMyReservations
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.user.UserId

class ListMyReservationsUseCase(
    private val reservations: ReservationRepository,
) : ListMyReservations {

    override fun list(userId: UserId): List<Reservation> = reservations.findByUserId(userId.value)
}
