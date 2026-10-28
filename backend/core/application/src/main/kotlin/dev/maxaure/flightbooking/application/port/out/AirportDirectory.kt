package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.catalogue.Airport

interface AirportDirectory {
    fun search(query: String, limit: Int): List<Airport>
}
