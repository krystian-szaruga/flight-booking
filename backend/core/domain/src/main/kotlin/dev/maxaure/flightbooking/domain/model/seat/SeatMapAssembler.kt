package dev.maxaure.flightbooking.domain.model.seat

import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.pricing.FareClass
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

object SeatMapAssembler {

    fun assemble(
        flightId: UUID,
        flightNumber: FlightNumber,
        origin: IataCode,
        destination: IataCode,
        departureTimeZone: ZoneId,
        departureAt: Instant,
        aircraft: String,
        status: String,
        fares: List<FareClass>,
        seats: List<SeatState>,
    ): SeatMap {
        val faresById = fares.associateBy { it.id }
        return SeatMap(
            flightId = flightId,
            flightNumber = flightNumber.value,
            origin = origin,
            destination = destination,
            departureTimeZone = departureTimeZone,
            departureAt = departureAt,
            aircraft = aircraft,
            status = status,
            fares = fares,
            seats = seats
                .map { seat ->
                    val fare = faresById.getValue(seat.fareClassId)
                    SeatView(
                        seatNumber = seat.seatNumber,
                        cabin = fare.cabin,
                        status = seat.status,
                        price = fare.priceForSeat(),
                    )
                }
                .sortedWith(SEAT_ORDER),
        )
    }

    private val SEAT_ORDER: Comparator<SeatView> =
        compareBy({ it.seatNumber.row }, { it.seatNumber.value })
}
