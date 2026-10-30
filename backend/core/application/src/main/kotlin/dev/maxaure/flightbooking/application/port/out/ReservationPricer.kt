package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.pricing.Price
import java.util.UUID

interface ReservationPricer {

    fun priceOf(reservationId: UUID): Price?
}
