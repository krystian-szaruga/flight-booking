package dev.maxaure.flightbooking.domain.model.seat

import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import dev.maxaure.flightbooking.domain.model.pricing.FareClass
import dev.maxaure.flightbooking.domain.model.pricing.Price
import java.time.Instant
import java.util.UUID

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

data class SeatState(
    val seatNumber: SeatNumber,
    val fareClassId: UUID,
    val status: SeatStatus,
)

data class SeatView(
    val seatNumber: SeatNumber,
    val cabin: Cabin,
    val status: SeatStatus,
    val price: Price,
)
