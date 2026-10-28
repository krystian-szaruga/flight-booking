package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.catalogue.Airport
import dev.maxaure.flightbooking.domain.model.codes.IataCode

interface AirportRepository {
    fun findByIataCode(code: IataCode): Airport?

    fun save(airport: Airport): Airport
}
