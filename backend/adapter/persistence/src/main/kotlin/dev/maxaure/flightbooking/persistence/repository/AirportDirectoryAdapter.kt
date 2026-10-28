package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.AirportDirectory
import dev.maxaure.flightbooking.domain.model.catalogue.Airport
import dev.maxaure.flightbooking.persistence.mapper.AirportMapper
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
internal class AirportDirectoryAdapter(
    private val jpa: SpringAirportJpaRepository,
) : AirportDirectory {

    override fun search(query: String, limit: Int): List<Airport> =
        jpa.findByIataCodeContainingIgnoreCaseOrCityContainingIgnoreCaseOrNameContainingIgnoreCase(
            iataCode = query,
            city = query,
            name = query,
            pageable = PageRequest.of(0, limit),
        ).map(AirportMapper::toDomain)
}
