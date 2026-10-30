package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.search.FlightDeal
import dev.maxaure.flightbooking.domain.model.search.FlightMatch
import dev.maxaure.flightbooking.domain.model.seat.SeatMap
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

interface SearchFlights {

    fun search(
        origin: String,
        destination: String,
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId,
        cabin: String? = null,
        currency: String? = null,
    ): List<FlightMatch>

    fun seatMap(flightId: UUID, currency: String? = null): SeatMap?

    fun deals(limit: Int = 6, currency: String? = null): List<FlightDeal>
}
