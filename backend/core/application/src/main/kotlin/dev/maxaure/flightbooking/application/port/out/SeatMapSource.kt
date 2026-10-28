package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.pricing.FareClass
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.domain.model.seat.SeatState
import java.time.Instant
import java.util.UUID

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
