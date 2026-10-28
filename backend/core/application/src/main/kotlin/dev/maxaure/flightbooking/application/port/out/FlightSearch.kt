package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.search.DealsQuery
import dev.maxaure.flightbooking.domain.model.search.FlightDeal
import dev.maxaure.flightbooking.domain.model.search.FlightMatch
import dev.maxaure.flightbooking.domain.model.search.SearchFlightsQuery
import java.util.UUID

interface FlightSearch {

    fun search(query: SearchFlightsQuery): List<FlightMatch>

    fun seatMap(flightId: UUID): SeatMapSource?

    fun deals(query: DealsQuery): List<FlightDeal>
}
