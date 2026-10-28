package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.SearchAirports
import dev.maxaure.flightbooking.application.port.out.AirportDirectory
import dev.maxaure.flightbooking.domain.model.catalogue.AirportSearchQuery
import dev.maxaure.flightbooking.domain.model.catalogue.AirportSummary

class SearchAirportsUseCase(
    private val directory: AirportDirectory,
) : SearchAirports {

    override fun search(rawQuery: String, limit: Int): List<AirportSummary> {
        val query = AirportSearchQuery.of(rawQuery, limit)
        if (query.isEmpty) return emptyList()
        return directory.search(query.text, query.limit)
            .map { it.toSummary() }
            .sortedWith(AirportSummary.BY_CODE)
    }
}
