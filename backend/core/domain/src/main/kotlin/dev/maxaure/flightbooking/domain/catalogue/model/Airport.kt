package dev.maxaure.flightbooking.domain.catalogue.model

import java.time.Instant
import java.util.UUID

data class Airport(
    val id: UUID,
    val iataCode: IataCode,
    val name: String,
    val city: String,
    val countryCode: CountryCode,
    val source: String = MANUAL_SOURCE,
    val syncedAt: Instant? = null,
) {
    init {
        require(name.isNotBlank()) { "Airport name must not be blank" }
        require(city.isNotBlank()) { "Airport city must not be blank" }
        require(source.isNotBlank()) { "Source must not be blank" }
    }

    companion object {
        const val MANUAL_SOURCE = "manual"
        fun create(
            iataCode: String,
            name: String,
            city: String,
            countryCode: String,
            id: UUID = UUID.randomUUID(),
            source: String = MANUAL_SOURCE,
            syncedAt: Instant = Instant.now(),
        ): Airport = Airport(
            id = id,
            iataCode = IataCode.of(iataCode),
            name = name.trim(),
            city = city.trim(),
            countryCode = CountryCode.of(countryCode),
            source = source,
            syncedAt = syncedAt,
        )
    }
}