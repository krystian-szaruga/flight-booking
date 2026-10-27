package dev.maxaure.flightbooking.application.catalogue.search

import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.catalogue.model.Cabin
import dev.maxaure.flightbooking.domain.catalogue.model.FareClass
import dev.maxaure.flightbooking.domain.catalogue.model.IataCode
import dev.maxaure.flightbooking.domain.catalogue.model.Price
import dev.maxaure.flightbooking.domain.catalogue.model.SeatNumber
import dev.maxaure.flightbooking.domain.catalogue.model.SeatStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class SearchFlightsQuery(
    val origin: IataCode,
    val destination: IataCode,
    val departureDate: LocalDate,
    val zone: ZoneId,
    val cabin: Cabin? = null,
) {
    init {
        if (origin == destination) {
            throw InvalidDomainValueException("Origin and destination must differ, got '$origin' twice")
        }
    }

    val from: Instant get() = departureDate.atStartOfDay(zone).toInstant()

    val until: Instant get() = departureDate.plusDays(1).atStartOfDay(zone).toInstant()
}

data class FlightMatch(
    val flightId: UUID,
    val flightNumber: String,
    val origin: IataCode,
    val destination: IataCode,
    val departureAt: Instant,
    val arrivalAt: Instant,
    val aircraft: String,
    val status: String,
    val fares: List<FareOption>,
) {
    val availableSeats: Int get() = fares.sumOf { it.seatsAvailable }
}

data class FareOption(
    val cabin: Cabin,
    val price: Price,
    val seatsAvailable: Int,
)

data class SeatMap(
    val flightId: UUID,
    val flightNumber: String,
    val origin: IataCode,
    val destination: IataCode,
    val departureAt: Instant,
    val aircraft: String,
    val status: String,
    val fares: List<FareClass>,
    val seats: List<SeatView>,
)

data class SeatView(
    val seatNumber: SeatNumber,
    val cabin: Cabin,
    val status: SeatStatus,
    val price: Price,
)
