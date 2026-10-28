package dev.maxaure.flightbooking.domain.model.catalogue

import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.model.pricing.FareClass
import java.time.Instant
import java.util.UUID

object FlightAssembler {

    fun fromFeed(
        record: FeedFlight,
        origin: Airport,
        destination: Airport,
        existing: Flight?,
        source: String,
        syncedAt: Instant,
    ): Flight {
        val fares = faresFrom(record.fares, existing)
        return Flight(
            id = existing?.id ?: UUID.randomUUID(),
            flightNumber = record.flightNumber,
            origin = origin,
            destination = destination,
            departureAt = record.departureAt,
            arrivalAt = record.arrivalAt,
            aircraft = record.aircraft,
            status = statusFrom(record.status, existing),
            fares = fares,
            seatRows = seatRowsFrom(record.seatRows, existing),
            source = source,
            syncedAt = syncedAt,
        )
    }

    private fun seatRowsFrom(override: Int?, existing: Flight?): Int =
        override ?: existing?.seatRows ?: Flight.DEFAULT_SEAT_ROWS

    private fun faresFrom(records: List<FeedFare>, existing: Flight?): List<FareClass> {
        if (records.isEmpty()) {
            throw InvalidDomainValueException("A flight must offer at least one fare class")
        }
        val existingByCabin = existing?.fares?.associateBy { it.cabin }.orEmpty()
        return records.map { fare ->
            FareClass(
                id = existingByCabin[fare.cabin]?.id ?: UUID.randomUUID(),
                cabin = fare.cabin,
                price = fare.price,
                multiplier = fare.multiplier,
            )
        }
    }

    private fun statusFrom(raw: String?, existing: Flight?): FlightStatus = when {
        raw != null -> FlightStatus.parse(raw)
        existing != null -> existing.status
        else -> FlightStatus.SCHEDULED
    }
}
