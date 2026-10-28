package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.SeatMapSource
import dev.maxaure.flightbooking.domain.model.search.DealsQuery
import dev.maxaure.flightbooking.domain.model.search.FareOption
import dev.maxaure.flightbooking.domain.model.search.FlightDeal
import dev.maxaure.flightbooking.domain.model.search.FlightMatch
import dev.maxaure.flightbooking.application.port.out.FlightSearch
import dev.maxaure.flightbooking.domain.model.search.SearchFlightsQuery
import dev.maxaure.flightbooking.domain.model.seat.SeatState
import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import dev.maxaure.flightbooking.domain.model.pricing.FareClass
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.domain.model.pricing.Price
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.seat.SeatStatus
import dev.maxaure.flightbooking.persistence.entity.FareClassEntity
import dev.maxaure.flightbooking.persistence.entity.FlightSeatEntity
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.util.UUID

@Transactional(readOnly = true)
internal class FlightSearchAdapter(
    private val search: SpringFlightSearchJpaRepository,
    private val seats: SpringSeatMapJpaRepository,
) : FlightSearch {

    override fun search(query: SearchFlightsQuery): List<FlightMatch> {
        val flights = search.findByRouteAndDay(
            origin = query.origin.value,
            destination = query.destination.value,
            from = query.from,
            until = query.until,
        )
        if (flights.isEmpty()) return emptyList()

        val availability = search.countAvailableByCabin(flights.map { it.id })
            .groupBy { it.flightId }
            .mapValues { (_, rows) -> rows.associate { Cabin.parse(it.cabin) to it.available.toInt() } }

        return flights.map { flight ->
            val availableByCabin = availability[flight.id].orEmpty()
            FlightMatch(
                flightId = flight.id,
                flightNumber = flight.flightNumber,
                origin = IataCode.fromTrusted(flight.origin.iataCode),
                destination = IataCode.fromTrusted(flight.destination.iataCode),
                departureAt = flight.departureAt,
                arrivalAt = flight.arrivalAt,
                aircraft = flight.aircraft,
                status = flight.status,
                fares = flight.fares.map { fare ->
                    FareOption(
                        cabin = Cabin.parse(fare.cabin),
                        price = Price(fare.basePriceAmount, fare.currency),
                        seatsAvailable = availableByCabin[Cabin.parse(fare.cabin)] ?: 0,
                    )
                }.sortedBy { it.cabin.ordinal },
            )
        }
    }

    override fun seatMap(flightId: UUID): SeatMapSource? {
        val flight = search.findById(flightId).orElse(null) ?: return null
        val fares = flight.fares.map(::fareToDomain)
        return SeatMapSource(
            flightId = flight.id,
            flightNumber = FlightNumber.fromTrusted(flight.flightNumber),
            origin = IataCode.fromTrusted(flight.origin.iataCode),
            destination = IataCode.fromTrusted(flight.destination.iataCode),
            departureAt = flight.departureAt,
            aircraft = flight.aircraft,
            status = flight.status,
            fares = fares,
            seats = seats.findByFlightId(flightId).map(::seatToState),
        )
    }

    override fun deals(query: DealsQuery): List<FlightDeal> =
        search.findDeals(
            from = query.from,
            pageable = PageRequest.of(0, query.limit),
        ).map { row ->
            FlightDeal(
                flightId = row.flightId,
                flightNumber = row.flightNumber,
                origin = IataCode.fromTrusted(row.origin),
                destination = IataCode.fromTrusted(row.destination),
                departureAt = row.departureAt,
                arrivalAt = row.arrivalAt,
                cabin = Cabin.parse(row.cabin),
                price = Price(row.priceAmount.setScale(2, RoundingMode.HALF_UP), row.currency),
                seatsAvailable = row.available.toInt(),
            )
        }

    private fun fareToDomain(fare: FareClassEntity): FareClass =
        FareClass(
            id = fare.id,
            cabin = Cabin.parse(fare.cabin),
            price = Price(fare.basePriceAmount, fare.currency),
            multiplier = fare.multiplier,
        )

    private fun seatToState(seat: FlightSeatEntity): SeatState = SeatState(
        seatNumber = SeatNumber.fromTrusted(seat.seatNumber),
        fareClassId = seat.fareClass.id,
        status = SeatStatus.valueOf(seat.status),
    )
}
