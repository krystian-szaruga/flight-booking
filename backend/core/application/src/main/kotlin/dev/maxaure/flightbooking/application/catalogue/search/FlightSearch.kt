package dev.maxaure.flightbooking.application.catalogue.search

import dev.maxaure.flightbooking.domain.catalogue.model.Cabin
import dev.maxaure.flightbooking.domain.catalogue.model.FareClass
import dev.maxaure.flightbooking.domain.catalogue.model.FlightNumber
import dev.maxaure.flightbooking.domain.catalogue.model.IataCode
import dev.maxaure.flightbooking.domain.catalogue.model.Price
import dev.maxaure.flightbooking.domain.catalogue.model.SeatNumber
import dev.maxaure.flightbooking.domain.catalogue.model.SeatStatus
import java.time.Instant
import java.util.UUID

interface FlightSearch {
    fun search(query: SearchFlightsQuery): List<FlightMatch>

    fun seatMap(flightId: UUID): SeatMapSource?

    fun deals(query: DealsQuery): List<FlightDeal>
}

data class DealsQuery(
    val from: Instant,
    val limit: Int = 6,
) {
    init {
        require(limit > 0) { "Limit must be positive" }
    }
}

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

data class SeatMapSource(
    val flightId: UUID,
    val flightNumber: FlightNumber,
    val origin: IataCode,
    val destination: IataCode,
    val departureAt: Instant,
    val aircraft: String,
    val status: String,
    val fares: List<FareClass>,
    val seats: List<SeatState>,
)

data class SeatState(
    val seatNumber: SeatNumber,
    val fareClassId: UUID,
    val status: SeatStatus,
)
