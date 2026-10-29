package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.SearchFlights
import dev.maxaure.flightbooking.application.port.out.FlightSearch
import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import dev.maxaure.flightbooking.domain.model.search.DealsQuery
import dev.maxaure.flightbooking.domain.model.search.FlightDeal
import dev.maxaure.flightbooking.domain.model.search.FlightMatch
import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.domain.model.search.SearchFlightsQuery
import dev.maxaure.flightbooking.domain.model.seat.SeatMap
import dev.maxaure.flightbooking.domain.model.seat.SeatMapAssembler
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class FlightSearchUseCase(
    private val search: FlightSearch,
    private val clock: Clock,
) : SearchFlights {

    override fun search(
        origin: String,
        destination: String,
        from: LocalDate,
        to: LocalDate,
        zone: ZoneId,
        cabin: String?,
    ): List<FlightMatch> {
        val query = SearchFlightsQuery(
            origin = IataCode.of(origin),
            destination = IataCode.of(destination),
            departureFrom = from,
            departureTo = to,
            zone = zone,
            cabin = cabin?.let(Cabin::parse),
        )
        if (query.isDepartedBy(clock.instant())) {
            throw InvalidDomainValueException("Cannot search flights that already departed, asked up to $to")
        }
        return query.matchesOf(search.search(query))
    }

    override fun deals(limit: Int): List<FlightDeal> {
        val query = DealsQuery(from = clock.instant(), limit = limit)
        return query.offersOf(search.deals(query))
    }

    override fun seatMap(flightId: UUID): SeatMap? =
        search.seatMap(flightId)?.let { source ->
            SeatMapAssembler.assemble(
                flightId = source.flightId,
                flightNumber = source.flightNumber,
                origin = source.origin,
                destination = source.destination,
                departureAt = source.departureAt,
                aircraft = source.aircraft,
                status = source.status,
                fares = source.fares,
                seats = source.seats,
            )
        }
}
