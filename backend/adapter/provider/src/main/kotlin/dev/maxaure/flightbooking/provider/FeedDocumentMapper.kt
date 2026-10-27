package dev.maxaure.flightbooking.provider

import dev.maxaure.flightbooking.domain.catalogue.model.Cabin
import dev.maxaure.flightbooking.domain.catalogue.model.FeedAirport
import dev.maxaure.flightbooking.domain.catalogue.model.FeedFare
import dev.maxaure.flightbooking.domain.catalogue.model.FeedFlight
import dev.maxaure.flightbooking.domain.catalogue.model.FeedPage
import dev.maxaure.flightbooking.domain.catalogue.model.FlightNumber
import dev.maxaure.flightbooking.domain.catalogue.model.Price

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
                        multiplier = fare.multiplier ?: java.math.BigDecimal.ONE,
                    )
                },
                seatRows = flight.seatRows,
                status = flight.status,
            )
        },
    )
}
