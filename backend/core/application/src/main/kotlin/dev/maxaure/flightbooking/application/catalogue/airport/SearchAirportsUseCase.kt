package dev.maxaure.flightbooking.application.catalogue.airport

import dev.maxaure.flightbooking.domain.catalogue.model.Airport

interface AirportDirectory {
    fun search(query: String, limit: Int): List<Airport>
}

data class AirportSummary(
    val iataCode: String,
    val name: String,
    val city: String,
    val countryCode: String,
)

class SearchAirportsUseCase(
    private val directory: AirportDirectory,
) {

    fun search(rawQuery: String, limit: Int = DEFAULT_LIMIT): List<AirportSummary> {
        val query = rawQuery.trim()
        if (query.isEmpty()) return emptyList()
        val capped = limit.coerceIn(1, MAX_LIMIT)
        return directory.search(query, capped)
            .map { airport ->
                AirportSummary(
                    iataCode = airport.iataCode.value,
                    name = airport.name,
                    city = airport.city,
                    countryCode = airport.countryCode.value,
                )
            }
            .sortedBy { it.iataCode }
    }

    private companion object {
        const val DEFAULT_LIMIT = 10
        const val MAX_LIMIT = 25
    }
}
