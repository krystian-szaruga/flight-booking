package dev.maxaure.flightbooking.domain.model.search

import dev.maxaure.flightbooking.domain.model.codes.IataCode
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class FlightMatch(
    val flightId: UUID,
    val flightNumber: String,
    val origin: IataCode,
    val destination: IataCode,
    val departureTimeZone: ZoneId,
    val departureAt: Instant,
    val arrivalAt: Instant,
    val aircraft: String,
    val status: String,
    val fares: List<FareOption>,
) {
    val availableSeats: Int get() = fares.sumOf { it.seatsAvailable }
}
