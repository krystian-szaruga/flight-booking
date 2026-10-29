package dev.maxaure.flightbooking.domain.model.catalogue

import dev.maxaure.flightbooking.domain.model.codes.CountryCode
import dev.maxaure.flightbooking.domain.model.codes.IataCode
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class Airport(
    val id: UUID,
    val iataCode: IataCode,
    val name: String,
    val city: String,
    val countryCode: CountryCode,
    val timeZone: ZoneId = AirportTimeZones.of(iataCode.value),
    val source: String = MANUAL_SOURCE,
    val syncedAt: Instant? = null,
) {
    init {
        require(name.isNotBlank()) { "Airport name must not be blank" }
        require(city.isNotBlank()) { "Airport city must not be blank" }
        require(source.isNotBlank()) { "Source must not be blank" }
    }

    fun toSummary(): AirportSummary = AirportSummary(
        iataCode = iataCode.value,
        name = name,
        city = city,
        countryCode = countryCode.value,
    )

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

        fun sync(record: FeedAirport, source: String, syncedAt: Instant, existing: Airport? = null): Airport =
            create(
                iataCode = record.iataCode,
                name = record.name,
                city = record.city,
                countryCode = record.countryCode,
                id = existing?.id ?: UUID.randomUUID(),
                source = source,
                syncedAt = syncedAt,
            )
    }
}