package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.payment.Payment
import dev.maxaure.flightbooking.domain.model.pricing.Price
import dev.maxaure.flightbooking.domain.model.reservation.Reservation

data class ReservationView(
    val reservation: Reservation,
    val price: Price?,
    val paid: Payment?,
)

interface DescribeReservation {

    fun describe(reservation: Reservation, currency: String? = null): ReservationView
}
