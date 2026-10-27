package dev.maxaure.flightbooking.application.catalogue

import dev.maxaure.flightbooking.application.catalogue.repository.AirportRepository
import dev.maxaure.flightbooking.application.catalogue.repository.FlightRepository
import dev.maxaure.flightbooking.application.catalogue.repository.IngestionRunRepository
import dev.maxaure.flightbooking.application.catalogue.repository.RecordWriter
import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.catalogue.model.*
import java.time.Clock
import java.time.Instant
import java.util.*

class CatalogueIngestionUseCase(
    private val airports: AirportRepository,
    private val flights: FlightRepository,
    private val runs: IngestionRunRepository,
    private val records: RecordWriter,
    private val clock: Clock,
) {

    fun ingest(source: String, page: FeedPage): IngestionSummary {
        val run = runs.save(IngestionRun.start(source, clock.instant()))
        val syncedAt = clock.instant()
        var rejected = 0
        var airportsWritten = 0
        var flightsWritten = 0

        val acceptedAirports = page.airports.mapNotNull { record ->
            val airport = attempt(rejecting = { rejected++ }) { upsertAirport(record, source, syncedAt) }
            if (airport != null) airportsWritten++
            airport
        }.associateBy { it.iataCode }

        page.flights.forEach { record ->
            val flight = attempt(rejecting = { rejected++ }) {
                upsertFlight(record, source, syncedAt, acceptedAirports)
            }
            if (flight != null) flightsWritten++
        }

        val status = if (rejected == 0) IngestionStatus.SUCCEEDED else IngestionStatus.PARTIAL
        val finished = run.finish(status, clock.instant()).copy(
            airportsWritten = airportsWritten,
            flightsWritten = flightsWritten,
            rejected = rejected,
        )
        runs.save(finished)
        return IngestionSummary(run.id, status, airportsWritten, flightsWritten, rejected)
    }

    private fun <T : Any> attempt(rejecting: () -> Unit, block: () -> T): T? =
        try {
            records.write(block)
        } catch (ex: RuntimeException) {
            rejecting()
            null
        }

    fun upsertAirport(record: FeedAirport, source: String, syncedAt: Instant): Airport {
        val candidate = Airport.create(
            iataCode = record.iataCode,
            name = record.name,
            city = record.city,
            countryCode = record.countryCode,
            source = source,
            syncedAt = syncedAt,
        )
        val existing = airports.findByIataCode(candidate.iataCode)
        return airports.save(if (existing == null) candidate else candidate.copy(id = existing.id))
    }

    fun upsertFlight(
        record: FeedFlight,
        source: String,
        syncedAt: Instant,
        knownAirports: Map<IataCode, Airport> = emptyMap(),
    ): Flight {
        val origin = resolveAirport(record.originIataCode, knownAirports)
        val destination = resolveAirport(record.destinationIataCode, knownAirports)
        if (origin.id == destination.id) {
            throw InvalidDomainValueException(
                "Flight ${record.flightNumber.value} cannot depart and arrive at ${origin.iataCode.value}",
            )
        }

        val existing = flights.findByIdentity(FlightIdentity(record.flightNumber, record.departureAt))
        val seatRows = record.seatRows ?: existing?.seatRows ?: Flight.DEFAULT_SEAT_ROWS
        if (seatRows < 1 || seatRows > Flight.MAX_SEAT_ROWS) {
            throw InvalidDomainValueException("Seat rows must be between 1 and ${Flight.MAX_SEAT_ROWS}")
        }

        val fares = buildFares(record.fares, existing)
        val flight = Flight(
            id = existing?.id ?: UUID.randomUUID(),
            flightNumber = record.flightNumber,
            origin = origin,
            destination = destination,
            departureAt = record.departureAt,
            arrivalAt = record.arrivalAt,
            aircraft = record.aircraft,
            status = resolveStatus(record.status, existing),
            fares = fares,
            seatRows = seatRows,
            source = source,
            syncedAt = syncedAt,
        )

        val saved = flights.save(flight)
        if (existing == null || existing.seatRows != seatRows) {
            flights.replaceSeatInventory(saved.id, SeatGenerator.generate(flight.copy(id = saved.id)))
        }
        return saved
    }

    private fun resolveAirport(code: String, known: Map<IataCode, Airport>): Airport {
        val iata = IataCode.of(code)
        return known[iata]
            ?: airports.findByIataCode(iata)
            ?: throw NotFoundException(
                "Airport $iata is not in the catalogue; ingest it before flights that use it",
            )
    }

    private fun buildFares(records: List<FeedFare>, existing: Flight?): List<FareClass> {
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

    private fun resolveStatus(raw: String?, existing: Flight?): FlightStatus = when {
        raw != null -> FlightStatus.parse(raw)
        existing != null -> existing.status
        else -> FlightStatus.SCHEDULED
    }
}

data class IngestionSummary(
    val runId: UUID,
    val status: IngestionStatus,
    val airports: Int,
    val flights: Int,
    val rejected: Int,
)
