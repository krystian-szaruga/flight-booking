package dev.maxaure.flightbooking.scheduler

import dev.maxaure.flightbooking.application.catalogue.CatalogueIngestionUseCase
import dev.maxaure.flightbooking.application.catalogue.feed.port.FeedRequest
import dev.maxaure.flightbooking.application.catalogue.feed.port.FlightDataProvider
import dev.maxaure.flightbooking.application.catalogue.repository.AirportRepository
import dev.maxaure.flightbooking.application.catalogue.repository.FlightRepository
import dev.maxaure.flightbooking.application.catalogue.repository.IngestionRunRepository
import dev.maxaure.flightbooking.application.catalogue.repository.RecordWriter
import dev.maxaure.flightbooking.domain.catalogue.model.Airport
import dev.maxaure.flightbooking.domain.catalogue.model.FeedPage
import dev.maxaure.flightbooking.domain.catalogue.model.Flight
import dev.maxaure.flightbooking.domain.catalogue.model.FlightIdentity
import dev.maxaure.flightbooking.domain.catalogue.model.IataCode
import dev.maxaure.flightbooking.domain.catalogue.model.IngestionRun
import dev.maxaure.flightbooking.domain.catalogue.model.IngestionStatus
import dev.maxaure.flightbooking.domain.catalogue.model.Seat
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedPullJobTest {

    private val now = Instant.parse("2026-10-26T08:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val runs = RecordingIngestionRunRepository()

    private fun job(vararg providers: FlightDataProvider) = FeedPullJob(
        providers.toList(),
        CatalogueIngestionUseCase(
            EmptyAirportRepository,
            EmptyFlightRepository,
            runs,
            DirectRecordWriter,
            clock,
        ),
        runs,
        horizonDays = 60,
        pullOnStartup = true,
        clock = clock,
    )

    @Test
    fun `when a provider throws then a failed run is recorded with the reason`() {
        val broken = object : FlightDataProvider {
            override val id = "broken"
            override fun fetch(query: FeedRequest): FeedPage = error("partner is down")
        }

        job(broken).pull()

        val run = runs.saved.single()
        assertEquals("broken", run.source)
        assertEquals(IngestionStatus.FAILED, run.status)
        assertEquals("partner is down", run.errorMessage)
        assertEquals(now, run.finishedAt)
    }

    @Test
    fun `when a provider succeeds then no separate failed run is recorded`() {
        val healthy = object : FlightDataProvider {
            override val id = "healthy"
            override fun fetch(query: FeedRequest): FeedPage = FeedPage(emptyList(), emptyList())
        }

        job(healthy).pull()

        assertTrue(runs.saved.all { it.status != IngestionStatus.FAILED })
        assertEquals(IngestionStatus.SUCCEEDED, runs.saved.last().status)
    }
}

private object DirectRecordWriter : RecordWriter {
    override fun <T : Any> write(block: () -> T): T = block()
}

private class RecordingIngestionRunRepository : IngestionRunRepository {
    val saved = mutableListOf<IngestionRun>()

    override fun save(run: IngestionRun): IngestionRun = run.also { saved += it }

    override fun findById(id: UUID): IngestionRun? = saved.lastOrNull { it.id == id }
}

private object EmptyAirportRepository : AirportRepository {
    override fun findByIataCode(code: IataCode): Airport? = null

    override fun save(airport: Airport): Airport = airport
}

private object EmptyFlightRepository : FlightRepository {
    override fun findById(id: UUID): Flight? = null

    override fun findByIdentity(identity: FlightIdentity): Flight? = null

    override fun save(flight: Flight): Flight = flight

    override fun replaceSeatInventory(flightId: UUID, seats: List<Seat>) = Unit

    override fun countSeats(flightId: UUID): Int = 0

    override fun findDeparturesBetween(from: Instant, before: Instant): List<Flight> = emptyList()
}
