package dev.maxaure.flightbooking.domain.model.search

import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.domain.model.pricing.Price
import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import java.time.Instant
import java.util.UUID

data class FlightDeal(
    val flightId: UUID,
    val flightNumber: String,
    val origin: IataCode,
    val destination: IataCode,
    val departureAt: Instant,
    val arrivalAt: Instant,
    val cabin: Cabin,
    val price: Price,
    val seatsAvailable: Int,
)
