package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.AirportRepository
import dev.maxaure.flightbooking.domain.model.catalogue.Airport
import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.persistence.mapper.AirportMapper

internal class AirportRepositoryAdapter(
    private val jpa: SpringAirportJpaRepository,
) : AirportRepository {

    override fun findByIataCode(code: IataCode): Airport? =
        jpa.findByIataCode(code.value)?.let(AirportMapper::toDomain)

    override fun save(airport: Airport): Airport {
        val entity = when (val existing = jpa.findById(airport.id).orElse(null)) {
            null -> AirportMapper.toEntity(airport)
            else -> existing.apply {
                iataCode = airport.iataCode.value
                name = airport.name
                city = airport.city
                countryCode = airport.countryCode.value
                source = airport.source
                syncedAt = airport.syncedAt
            }
        }
        return AirportMapper.toDomain(jpa.save(entity))
    }
}
