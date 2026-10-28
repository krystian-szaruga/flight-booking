package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.catalogue.AirportSummary

interface SearchAirports {

    fun search(rawQuery: String, limit: Int = 10): List<AirportSummary>
}
