package dev.maxaure.flightbooking.web.catalogue

import dev.maxaure.flightbooking.application.catalogue.search.FlightDeal
import java.math.BigDecimal
import java.time.Instant

internal data class DealResponse(
    val flightId: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureAt: Instant,
    val arrivalAt: Instant,
    val cabin: String,
    val price: BigDecimal,
    val currency: String,
    val seatsAvailable: Int,
) {
    companion object {
        fun from(deal: FlightDeal) = DealResponse(
            flightId = deal.flightId.toString(),
            flightNumber = deal.flightNumber,
            origin = deal.origin.value,
            destination = deal.destination.value,
            departureAt = deal.departureAt,
            arrivalAt = deal.arrivalAt,
            cabin = deal.cabin.name,
            price = deal.price.amount,
            currency = deal.price.currency,
            seatsAvailable = deal.seatsAvailable,
        )
    }
}
