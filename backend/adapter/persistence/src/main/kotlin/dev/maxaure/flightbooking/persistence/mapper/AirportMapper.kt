package dev.maxaure.flightbooking.persistence.mapper

import dev.maxaure.flightbooking.domain.model.catalogue.Airport
import dev.maxaure.flightbooking.domain.model.catalogue.AirportTimeZones
import dev.maxaure.flightbooking.domain.model.codes.CountryCode
import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.persistence.entity.AirportEntity
import java.time.ZoneId

internal object AirportMapper {

    fun toDomain(entity: AirportEntity): Airport = Airport(
        id = entity.id,
        iataCode = IataCode.fromTrusted(entity.iataCode),
        name = entity.name,
        city = entity.city,
        countryCode = CountryCode.fromTrusted(entity.countryCode),
        timeZone = entity.timeZone?.let(ZoneId::of) ?: AirportTimeZones.of(entity.iataCode),
        source = entity.source,
        syncedAt = entity.syncedAt,
    )

    fun toEntity(airport: Airport): AirportEntity = AirportEntity(
        id = airport.id,
        iataCode = airport.iataCode.value,
        name = airport.name,
        city = airport.city,
        countryCode = airport.countryCode.value,
        timeZone = airport.timeZone.id,
        source = airport.source,
        syncedAt = airport.syncedAt,
    )
}
