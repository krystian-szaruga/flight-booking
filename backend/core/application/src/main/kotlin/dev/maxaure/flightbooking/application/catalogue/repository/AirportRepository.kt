package dev.maxaure.flightbooking.application.catalogue.repository

import dev.maxaure.flightbooking.domain.catalogue.model.Airport
import dev.maxaure.flightbooking.domain.catalogue.model.FeedAirport
import dev.maxaure.flightbooking.domain.catalogue.model.IataCode
import java.time.Instant

interface AirportRepository {
    fun findByIataCode(code: IataCode): Airport?

    fun save(airport: Airport): Airport
}
