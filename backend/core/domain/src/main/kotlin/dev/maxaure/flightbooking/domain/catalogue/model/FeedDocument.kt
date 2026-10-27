package dev.maxaure.flightbooking.domain.catalogue.model

import java.math.BigDecimal
import java.time.Instant

data class FeedPage(
    val airports: List<FeedAirport>,
    val flights: List<FeedFlight>,
) {
    val isEmpty: Boolean get() = airports.isEmpty() && flights.isEmpty()
}

data class FeedAirport(
    val iataCode: String,
    val name: String,
    val city: String,
    val countryCode: String,
)

data class FeedFlight(
    val flightNumber: FlightNumber,
    val originIataCode: String,
    val destinationIataCode: String,
    val departureAt: Instant,
    val arrivalAt: Instant,
    val aircraft: String,
    val fares: List<FeedFare>,
    val seatRows: Int? = null,
    val status: String? = null,
)

data class FeedFare(
    val cabin: Cabin,
    val price: Price,
    val multiplier: BigDecimal = BigDecimal.ONE,
)