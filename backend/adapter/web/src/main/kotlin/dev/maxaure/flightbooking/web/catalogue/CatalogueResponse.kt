package dev.maxaure.flightbooking.web.catalogue

import dev.maxaure.flightbooking.domain.model.search.FareOption
import dev.maxaure.flightbooking.domain.model.search.FlightMatch
import dev.maxaure.flightbooking.domain.model.seat.SeatMap
import dev.maxaure.flightbooking.domain.model.seat.SeatView
import dev.maxaure.flightbooking.domain.model.pricing.FareClass
import java.math.BigDecimal
import java.time.Instant

internal data class FareResponse(
    val cabin: String,
    val price: BigDecimal,
    val currency: String,
    val seatsAvailable: Int,
) {
    companion object {
        fun from(option: FareOption) = FareResponse(
            cabin = option.cabin.name,
            price = option.price.amount,
            currency = option.price.currency,
            seatsAvailable = option.seatsAvailable,
        )

        fun fromClass(fare: FareClass) = FareResponse(
            cabin = fare.cabin.name,
            price = fare.price.amount,
            currency = fare.price.currency,
            seatsAvailable = 0,
        )
    }
}

internal data class FlightSummaryResponse(
    val flightId: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTimeZone: String,
    val departureAt: Instant,
    val arrivalAt: Instant,
    val aircraft: String,
    val status: String,
    val availableSeats: Int,
    val fares: List<FareResponse>,
) {
    companion object {
        fun from(match: FlightMatch) = FlightSummaryResponse(
            flightId = match.flightId.toString(),
            flightNumber = match.flightNumber,
            origin = match.origin.value,
            destination = match.destination.value,
            departureTimeZone = match.departureTimeZone.id,
            departureAt = match.departureAt,
            arrivalAt = match.arrivalAt,
            aircraft = match.aircraft,
            status = match.status,
            availableSeats = match.availableSeats,
            fares = match.fares.map(FareResponse::from),
        )
    }
}

internal data class SeatResponse(
    val seat: String,
    val cabin: String,
    val price: BigDecimal,
    val currency: String,
    val status: String,
) {
    companion object {
        fun from(view: SeatView) = SeatResponse(
            seat = view.seatNumber.value,
            cabin = view.cabin.name,
            price = view.price.amount,
            currency = view.price.currency,
            status = view.status.name,
        )
    }
}

internal data class SeatMapResponse(
    val flightId: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTimeZone: String,
    val departureAt: Instant,
    val aircraft: String,
    val status: String,
    val seatRows: Int,
    val fares: List<FareResponse>,
    val seats: List<SeatResponse>,
) {
    companion object {
        fun from(map: SeatMap) = SeatMapResponse(
            flightId = map.flightId.toString(),
            flightNumber = map.flightNumber,
            origin = map.origin.value,
            destination = map.destination.value,
            departureTimeZone = map.departureTimeZone.id,
            departureAt = map.departureAt,
            aircraft = map.aircraft,
            status = map.status,
            seatRows = map.seats.maxOfOrNull { it.seatNumber.row } ?: 0,
            fares = map.fares.map(FareResponse::fromClass),
            seats = map.seats.map(SeatResponse::from),
        )
    }
}
