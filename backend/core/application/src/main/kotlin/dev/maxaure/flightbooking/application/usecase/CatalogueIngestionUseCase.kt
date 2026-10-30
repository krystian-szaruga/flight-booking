package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.IngestCatalogue
import dev.maxaure.flightbooking.domain.model.catalogue.IngestionSummary
import dev.maxaure.flightbooking.application.port.out.AirportRepository
import dev.maxaure.flightbooking.application.port.out.FlightRepository
import dev.maxaure.flightbooking.application.port.out.IngestionRunRepository
import dev.maxaure.flightbooking.application.port.out.NotificationAudience
import dev.maxaure.flightbooking.application.port.out.NotificationPublisher
import dev.maxaure.flightbooking.application.port.out.RecordWriter
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.catalogue.Airport
import dev.maxaure.flightbooking.domain.model.catalogue.FeedAirport
import dev.maxaure.flightbooking.domain.model.catalogue.FeedFlight
import dev.maxaure.flightbooking.domain.model.catalogue.FeedPage
import dev.maxaure.flightbooking.domain.model.catalogue.Flight
import dev.maxaure.flightbooking.domain.model.catalogue.FlightAssembler
import dev.maxaure.flightbooking.domain.model.catalogue.FlightIdentity
import dev.maxaure.flightbooking.domain.model.catalogue.FlightStatus
import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.domain.model.catalogue.IngestionRun
import java.time.Clock
import java.time.Instant

class CatalogueIngestionUseCase(
    private val airports: AirportRepository,
    private val flights: FlightRepository,
    private val runs: IngestionRunRepository,
    private val records: RecordWriter,
    private val audience: NotificationAudience,
    private val notifications: NotificationPublisher,
    private val clock: Clock,
) : IngestCatalogue {

    override fun ingest(source: String, page: FeedPage): IngestionSummary {
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

        val finished = run.recordIngested(airportsWritten, flightsWritten, rejected, clock.instant())
        runs.save(finished)
        return IngestionSummary(run.id, finished.status, airportsWritten, flightsWritten, rejected)
    }
    private fun <T : Any> attempt(rejecting: () -> Unit, block: () -> T): T? =
        try {
            records.write(block)
        } catch (ex: RuntimeException) {
            rejecting()
            null
        }

    fun upsertAirport(record: FeedAirport, source: String, syncedAt: Instant): Airport {
        val candidate = Airport.sync(record, source, syncedAt)
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
        val existing = flights.findByIdentity(FlightIdentity(record.flightNumber, record.departureAt))
        val flight = FlightAssembler.fromFeed(record, origin, destination, existing, source, syncedAt)

        val saved = flights.save(flight)
        val taken = flights.countSoldSeats(saved.id) > 0
        if (!taken && (existing == null || existing.seatRows != flight.seatRows)) {
            flights.replaceSeatInventory(saved.id, saved.seatInventory())
        }
        if (existing != null && existing.status != saved.status && saved.status.isDisruption()) {
            audience.disruptedPassengers(saved).forEach(notifications::publish)
        }
        return saved
    }

    private fun FlightStatus.isDisruption(): Boolean = this == FlightStatus.DELAYED || this == FlightStatus.CANCELLED

    private fun resolveAirport(code: String, known: Map<IataCode, Airport>): Airport {
        val iata = IataCode.of(code)
        return known[iata]
            ?: airports.findByIataCode(iata)
            ?: throw NotFoundException(
                "Airport $iata is not in the catalogue; ingest it before flights that use it",
            )
    }
}
