package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.DescribeReservation
import dev.maxaure.flightbooking.application.port.`in`.ReservationView
import dev.maxaure.flightbooking.application.port.out.CurrencyConverter
import dev.maxaure.flightbooking.application.port.out.PaymentRepository
import dev.maxaure.flightbooking.application.port.out.ReservationPricer
import dev.maxaure.flightbooking.domain.model.reservation.Reservation

class DescribeReservationUseCase(
    private val pricer: ReservationPricer,
    private val payments: PaymentRepository,
    private val converter: CurrencyConverter,
) : DescribeReservation {

    override fun describe(reservation: Reservation, currency: String?): ReservationView =
        ReservationView(
            reservation = reservation,
            price = pricer.priceOf(reservation.id)?.let { converter.convert(it, currency) },
            paid = payments.findLatestApproved(reservation.id),
        )
}
