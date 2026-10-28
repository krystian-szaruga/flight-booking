package dev.maxaure.flightbooking.web.airport

import dev.maxaure.flightbooking.domain.model.catalogue.AirportSummary

internal data class AirportSummaryResponse(
    val iataCode: String,
    val name: String,
    val city: String,
    val countryCode: String,
) {
    companion object {
        fun from(summary: AirportSummary) = AirportSummaryResponse(
            iataCode = summary.iataCode,
            name = summary.name,
            city = summary.city,
            countryCode = summary.countryCode,
        )
    }
}
