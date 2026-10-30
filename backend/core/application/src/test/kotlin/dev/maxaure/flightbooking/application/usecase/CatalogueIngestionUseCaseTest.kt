package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.AirportRepository
import dev.maxaure.flightbooking.application.port.out.FlightRepository
import dev.maxaure.flightbooking.application.port.out.IngestionRunRepository
import dev.maxaure.flightbooking.application.port.out.NotificationAudience
import dev.maxaure.flightbooking.application.port.out.NotificationPublisher
import dev.maxaure.flightbooking.application.port.out.RecordWriter
import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.catalogue.Airport
import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import dev.maxaure.flightbooking.domain.model.codes.CountryCode
import dev.maxaure.flightbooking.domain.model.catalogue.FeedAirport
import dev.maxaure.flightbooking.domain.model.catalogue.FeedFare
import dev.maxaure.flightbooking.domain.model.catalogue.FeedFlight
import dev.maxaure.flightbooking.domain.model.catalogue.FeedPage
import dev.maxaure.flightbooking.domain.model.catalogue.Flight
import dev.maxaure.flightbooking.domain.model.catalogue.FlightIdentity
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.catalogue.IngestionRun
import dev.maxaure.flightbooking.domain.model.catalogue.IngestionStatus
import dev.maxaure.flightbooking.domain.model.notification.NotificationEvent
import dev.maxaure.flightbooking.domain.model.pricing.Price
import dev.maxaure.flightbooking.domain.model.seat.Seat
import dev.maxaure.flightbooking.domain.model.user.Email
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CatalogueIngestionUseCaseTest {

    private val now = Instant.parse("2026-10-26T08:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private val airports = InMemoryAirportRepository()
    private val flights = InMemoryFlightRepository()
    private val runs = InMemoryIngestionRunRepository()
    private val audience = InMemoryNotificationAudience()
    private val notifications = RecordingNotificationPublisher()
    private val service = CatalogueIngestionUseCase(
        airports, flights, runs, DirectRecordWriter, audience, notifications, clock,
    )

    private fun upsertAirport(record: FeedAirport, source: String = SOURCE) =
        service.upsertAirport(record, source, now)

    private fun upsertFlight(record: FeedFlight, source: String = SOURCE) =
        service.upsertFlight(record, source, now)

    private fun fare(cabin: Cabin, amount: String) = FeedFare(cabin, Price(BigDecimal(amount), "PLN"))

    private fun feedFlight(
        number: String = "LO381",
        origin: String = "WAW",
        destination: String = "LIS",
        departure: Instant = Instant.parse("2026-11-01T08:00:00Z"),
        fares: List<FeedFare> = listOf(fare(Cabin.ECONOMY, "399.00")),
        seatRows: Int? = 10,
        status: String? = null,
    ) = FeedFlight(
        flightNumber = FlightNumber.of(number),
        originIataCode = origin,
        destinationIataCode = destination,
        departureAt = departure,
        arrivalAt = departure.plus(Duration.ofHours(4)),
        aircraft = "A320neo",
        fares = fares,
        seatRows = seatRows,
        status = status,
    )

    private fun seedAirports() {
        upsertAirport(FeedAirport("WAW", "Warsaw Chopin", "Warsaw", "PL"))
        upsertAirport(FeedAirport("LIS", "Lisbon", "Lisbon", "PT"))
    }

    @Test
    fun `when the same feed page is delivered twice then the flight is updated and its seats are not duplicated`() {
        seedAirports()

        val first = upsertFlight(feedFlight())
        val second = upsertFlight(feedFlight())

        assertEquals(first.id, second.id)
        assertEquals(1, flights.distinctFlights)
        assertEquals(60, flights.countSeats(first.id))
    }

    @Test
    fun `when a flight is re-delivered with a new price then the fare keeps its id and takes the new amount`() {
        seedAirports()
        val created = upsertFlight(feedFlight(fares = listOf(fare(Cabin.ECONOMY, "399.00"))))

        val updated = upsertFlight(feedFlight(fares = listOf(fare(Cabin.ECONOMY, "549.00"))))

        assertEquals(created.fares.single().id, updated.fares.single().id)
        assertEquals(BigDecimal("549.00"), updated.fares.single().price.amount)
        assertEquals(60, flights.countSeats(created.id))
    }

    @Test
    fun `when an override increases the seat layout then the inventory is regenerated at the new size`() {
        seedAirports()
        val created = upsertFlight(feedFlight(seatRows = 10))

        val resized = upsertFlight(feedFlight(seatRows = 20))

        assertEquals(created.id, resized.id)
        assertEquals(120, flights.countSeats(created.id))
    }

    @Test
    fun `when a flight with a held seat is delivered again with a new layout then the held inventory is kept`() {
        seedAirports()
        val created = upsertFlight(feedFlight(seatRows = 10))
        flights.hold(created.id, "1A")

        val redelivered = upsertFlight(feedFlight(seatRows = 20))

        assertEquals(created.id, redelivered.id)
        assertEquals(60, flights.countSeats(created.id))
        assertEquals(1, flights.countSoldSeats(created.id))
    }

    @Test
    fun `when a flight references an airport that was never ingested then ingestion fails`() {
        upsertAirport(FeedAirport("WAW", "Warsaw Chopin", "Warsaw", "PL"))

        assertFailsWith<NotFoundException> { upsertFlight(feedFlight()) }
    }

    @Test
    fun `when a flight departs and arrives at the same airport then ingestion rejects it`() {
        seedAirports()

        assertFailsWith<InvalidDomainValueException> { upsertFlight(feedFlight(destination = "WAW")) }
    }

    @Test
    fun `when a later page omits the status then the stored delay is preserved`() {
        seedAirports()
        upsertFlight(feedFlight(status = "DELAYED"))

        val resubmitted = upsertFlight(feedFlight())

        assertEquals("DELAYED", resubmitted.status.name)
    }

    @Test
    fun `when a scheduled flight is reported delayed then every passenger on it is notified`() {
        seedAirports()
        upsertFlight(feedFlight())

        upsertFlight(feedFlight(status = "DELAYED"))

        assertEquals(2, notifications.events.size)
        assertEquals(1, audience.queries)
    }

    @Test
    fun `when a flight is reingested with the same status then no disruption notification is sent`() {
        seedAirports()
        upsertFlight(feedFlight())

        upsertFlight(feedFlight())

        assertEquals(emptyList(), notifications.events)
        assertEquals(0, audience.queries)
    }

    @Test
    fun `when a page carries airports and flights together then flights resolve against that same page`() {
        val summary = service.ingest(
            SOURCE,
            FeedPage(
                airports = listOf(
                    FeedAirport("WAW", "Warsaw Chopin", "Warsaw", "PL"),
                    FeedAirport("LIS", "Lisbon", "Lisbon", "PT"),
                ),
                flights = listOf(feedFlight()),
            ),
        )

        assertEquals(2, summary.airports)
        assertEquals(1, summary.flights)
        assertEquals(0, summary.rejected)
        assertEquals(IngestionStatus.SUCCEEDED, summary.status)
        assertEquals(60, flights.allSeats().size)
    }

    @Test
    fun `when a page contains a broken record then the good records are kept and the run is partial`() {
        val summary = service.ingest(
            SOURCE,
            FeedPage(
                airports = listOf(
                    FeedAirport("WAW", "Warsaw Chopin", "Warsaw", "PL"),
                    FeedAirport("LIS", "Lisbon", "Lisbon", "PT"),
                    FeedAirport("XX", "Broken", "Nowhere", "PL"),
                ),
                flights = listOf(
                    feedFlight(),
                    feedFlight(number = "LO999", destination = "WAW"),
                ),
            ),
        )

        assertEquals(2, summary.airports)
        assertEquals(1, summary.flights)
        assertEquals(2, summary.rejected)
        assertEquals(IngestionStatus.PARTIAL, summary.status)
        assertEquals(1, flights.distinctFlights)
        assertEquals(IngestionStatus.PARTIAL, runs.findById(summary.runId)!!.status)
    }

    @Test
    fun `when a page is fully accepted then the recorded run holds the counters and the source`() {
        val summary = service.ingest(
            "file",
            FeedPage(
                airports = listOf(FeedAirport("WAW", "Warsaw Chopin", "Warsaw", "PL")),
                flights = emptyList(),
            ),
        )

        val run = runs.findById(summary.runId)!!

        assertEquals("file", run.source)
        assertEquals(IngestionStatus.SUCCEEDED, run.status)
        assertEquals(1, run.airportsWritten)
        assertEquals(0, run.flightsWritten)
        assertEquals(0, run.rejected)
        assertEquals(now, run.startedAt)
        assertEquals(now, run.finishedAt)
    }

    @Test
    fun `when an airport is re-ingested with corrected details then its id survives and existing flights still point at it`() {
        seedAirports()
        val flight = upsertFlight(feedFlight())

        val updated = upsertAirport(FeedAirport("waw", "Warsaw Chopin Airport", "Warsaw", "pl"))

        assertEquals("Warsaw Chopin Airport", updated.name)
        assertEquals(CountryCode.of("PL"), updated.countryCode)
        assertEquals(flight.origin.id, updated.id)
    }

    @Test
    fun `when a page is ingested then airports and flights record the source and sync time`() {
        service.ingest(
            "file",
            FeedPage(
                airports = listOf(
                    FeedAirport("WAW", "Warsaw Chopin", "Warsaw", "PL"),
                    FeedAirport("LIS", "Lisbon", "Lisbon", "PT"),
                ),
                flights = listOf(feedFlight()),
            ),
        )

        val airport = airports.findByIataCode(IataCode.of("WAW"))!!
        val flight = flights.findByIdentity(
            FlightIdentity(FlightNumber.of("LO381"), Instant.parse("2026-11-01T08:00:00Z")),
        )!!

        assertEquals("file", airport.source)
        assertEquals(now, airport.syncedAt)
        assertEquals("file", flight.source)
        assertEquals(now, flight.syncedAt)
    }

    private companion object {
        const val SOURCE = "test"
    }
}

private class InMemoryAirportRepository : AirportRepository {
    private val byCode = LinkedHashMap<IataCode, Airport>()

    override fun findByIataCode(code: IataCode): Airport? = byCode[code]

    override fun save(airport: Airport): Airport = airport.also { byCode[it.iataCode] = it }
}

private class InMemoryFlightRepository : FlightRepository {
    private val byIdentity = LinkedHashMap<FlightIdentity, Flight>()
    private val seats = mutableMapOf<UUID, List<Seat>>()
    private val held = mutableMapOf<UUID, MutableSet<String>>()

    fun hold(flightId: UUID, seatNumber: String) {
        held.getOrPut(flightId) { mutableSetOf() }.add(seatNumber)
    }

    val distinctFlights: Int get() = byIdentity.size

    override fun findById(id: UUID): Flight? = byIdentity.values.firstOrNull { it.id == id }

    override fun findByIdentity(identity: FlightIdentity): Flight? = byIdentity[identity]

    override fun save(flight: Flight): Flight = flight.also { byIdentity[it.identity] = it }

    override fun replaceSeatInventory(flightId: UUID, seats: List<Seat>) {
        this.seats[flightId] = seats
    }

    override fun countSeats(flightId: UUID): Int = seats[flightId]?.size ?: 0

    override fun countSoldSeats(flightId: UUID): Int = held[flightId]?.size ?: 0

    override fun findDeparturesBetween(from: Instant, before: Instant): List<Flight> =
        byIdentity.values.filter { !it.departureAt.isBefore(from) && it.departureAt.isBefore(before) }

    fun allSeats(): List<Seat> = seats.values.flatten()
}

private object DirectRecordWriter : RecordWriter {
    override fun <T : Any> write(block: () -> T): T = block()
}

private class InMemoryNotificationAudience : NotificationAudience {
    var queries = 0

    override fun paymentReminders(
        now: Instant,
        expiresBefore: Instant,
        limit: Int,
    ): List<NotificationEvent.PaymentReminder> = emptyList()

    override fun departureReminders(
        now: Instant,
        departsBefore: Instant,
        limit: Int,
    ): List<NotificationEvent.DepartureReminder> = emptyList()

    override fun disruptedPassengers(flight: Flight): List<NotificationEvent.FlightDisrupted> {
        queries++
        return listOf("ada@example.com", "bob@example.com").mapIndexed { index, email ->
            NotificationEvent.FlightDisrupted(
                recipient = Email.of(email),
                reservationId = UUID.randomUUID(),
                flightNumber = flight.flightNumber,
                departureAt = flight.departureAt,
                seats = listOf(SeatNumber.of(1, 'A' + index)),
                status = flight.status,
            )
        }
    }

    override fun markPaymentReminderSent(reservationId: UUID, at: Instant) = Unit

    override fun markDepartureReminderSent(reservationId: UUID, at: Instant) = Unit
}

private class RecordingNotificationPublisher : NotificationPublisher {
    val events = mutableListOf<NotificationEvent>()

    override fun publish(event: NotificationEvent) {
        events += event
    }
}

private class InMemoryIngestionRunRepository : IngestionRunRepository {
    private val byId = LinkedHashMap<UUID, IngestionRun>()

    override fun save(run: IngestionRun): IngestionRun = run.also { byId[it.id] = it }

    override fun findById(id: UUID): IngestionRun? = byId[id]
}