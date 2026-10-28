package dev.maxaure.flightbooking.application.catalogue.search

import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.catalogue.model.Cabin
import dev.maxaure.flightbooking.domain.catalogue.model.IataCode
import dev.maxaure.flightbooking.domain.catalogue.model.Price
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class FlightSearchUseCase(
    private val search: FlightSearch,
    private val clock: Clock,
) {

    fun search(
        origin: String,
        destination: String,
        date: LocalDate,
        zone: ZoneId,
        cabin: String? = null,
    ): List<FlightMatch> {
        val query = SearchFlightsQuery(
            origin = IataCode.of(origin),
            destination = IataCode.of(destination),
            departureDate = date,
            zone = zone,
            cabin = cabin?.let(Cabin::parse),
        )
        if (!query.until.isAfter(clock.instant())) {
            throw InvalidDomainValueException("Cannot search flights that already departed, asked for $date")
        }
        return search.search(query)
            .map { match ->
                if (query.cabin == null) match
                else match.copy(fares = match.fares.filter { it.cabin == query.cabin })
            }
            .filter { it.fares.isNotEmpty() }
            .sortedWith(compareBy({ it.departureAt }, { it.flightNumber }))
    }

    fun deals(limit: Int = 6): List<FlightDeal> {
        if (limit !in 1..MAX_DEALS) {
            throw InvalidDomainValueException("Limit must be between 1 and $MAX_DEALS, got $limit")
        }
        val now = clock.instant()
        return search.deals(DealsQuery(now, limit))
            .filter { it.seatsAvailable > 0 && it.departureAt.isAfter(now) }
            .sortedWith(compareBy({ it.price.amount }, { it.price.currency }, { it.departureAt }))
            .take(limit)
    }

    fun seatMap(flightId: UUID): SeatMap {
        val source = search.seatMap(flightId)
            ?: throw NotFoundException("Flight $flightId is not in the catalogue")
        val faresById = source.fares.associateBy { it.id }
        return SeatMap(
            flightId = source.flightId,
            flightNumber = source.flightNumber.value,
            origin = source.origin,
            destination = source.destination,
            departureAt = source.departureAt,
            aircraft = source.aircraft,
            status = source.status,
            fares = source.fares,
            seats = source.seats
                .map { seat ->
                    val fare = faresById.getValue(seat.fareClassId)
                    SeatView(
                        seatNumber = seat.seatNumber,
                        cabin = fare.cabin,
                        status = seat.status,
                        price = Price(
                            fare.price.amount.multiply(fare.multiplier).setScale(2, RoundingMode.HALF_UP),
                            fare.price.currency,
                        ),
                    )
                }
                .sortedWith(compareBy({ it.seatNumber.row }, { it.seatNumber.value })),
        )
    }

    private companion object {
        const val MAX_DEALS = 50
    }
}
