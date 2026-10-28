package dev.maxaure.flightbooking.provider

import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import dev.maxaure.flightbooking.domain.model.catalogue.FeedAirport
import dev.maxaure.flightbooking.domain.model.catalogue.FeedFare
import dev.maxaure.flightbooking.domain.model.catalogue.FeedFlight
import dev.maxaure.flightbooking.domain.model.catalogue.FeedPage
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.pricing.Price
import java.math.BigDecimal

internal object FeedDocumentMapper {

    fun toDomain(dto: FeedDocumentDto): FeedPage = FeedPage(
        airports = dto.airports.map { FeedAirport(it.iataCode, it.name, it.city, it.countryCode) },
        flights = dto.flights.map { flight ->
            FeedFlight(
                flightNumber = FlightNumber.of(flight.flightNumber),
                originIataCode = flight.origin,
                destinationIataCode = flight.destination,
                departureAt = flight.departureAt,
                arrivalAt = flight.arrivalAt,
                aircraft = flight.aircraft,
                fares = flight.fares.map { fare ->
                    FeedFare(
                        cabin = Cabin.parse(fare.cabin),
                        price = Price(fare.amount, fare.currency),
                        multiplier = fare.multiplier ?: BigDecimal.ONE,
                    )
                },
                seatRows = flight.seatRows,
                status = flight.status,
            )
        },
    )
}
