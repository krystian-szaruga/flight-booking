package dev.maxaure.flightbooking.persistence.mapper

import dev.maxaure.flightbooking.domain.catalogue.model.Airport
import dev.maxaure.flightbooking.domain.catalogue.model.CountryCode
import dev.maxaure.flightbooking.domain.catalogue.model.IataCode
import dev.maxaure.flightbooking.persistence.entity.AirportEntity

internal object AirportMapper {

    fun toDomain(entity: AirportEntity): Airport = Airport(
        id = entity.id,
        iataCode = IataCode.fromTrusted(entity.iataCode),
        name = entity.name,
        city = entity.city,
        countryCode = CountryCode.fromTrusted(entity.countryCode),
        source = entity.source,
        syncedAt = entity.syncedAt,
    )

    fun toEntity(airport: Airport): AirportEntity = AirportEntity(
        id = airport.id,
        iataCode = airport.iataCode.value,
        name = airport.name,
        city = airport.city,
        countryCode = airport.countryCode.value,
        source = airport.source,
        syncedAt = airport.syncedAt,
    )
}
