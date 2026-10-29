package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.FlightSearch
import dev.maxaure.flightbooking.application.port.out.SeatMapSource
import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import dev.maxaure.flightbooking.domain.model.search.DealsQuery
import dev.maxaure.flightbooking.domain.model.pricing.FareClass
import dev.maxaure.flightbooking.domain.model.search.FareOption
import dev.maxaure.flightbooking.domain.model.search.FlightDeal
import dev.maxaure.flightbooking.domain.model.search.FlightMatch
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.domain.model.pricing.Price
import dev.maxaure.flightbooking.domain.model.search.SearchFlightsQuery
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.seat.SeatState
import dev.maxaure.flightbooking.domain.model.seat.SeatStatus
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FlightSearchUseCaseTest {

    private val now = Instant.parse("2026-10-27T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val warsaw = ZoneId.of("Europe/Warsaw")
    private val date = LocalDate.parse("2026-11-01")

    private val economy = FareClass(
        id = UUID.randomUUID(),
        cabin = Cabin.ECONOMY,
        price = Price(BigDecimal("400.00"), "PLN"),
        multiplier = BigDecimal("1.00"),
    )
    private val business = FareClass(
        id = UUID.randomUUID(),
        cabin = Cabin.BUSINESS,
        price = Price(BigDecimal("400.00"), "PLN"),
        multiplier = BigDecimal("2.50"),
    )

    private fun match(
        number: String,
        departureAt: Instant,
        fares: List<FareOption> = listOf(FareOption(Cabin.ECONOMY, economy.price, 10)),
    ) = FlightMatch(
        flightId = UUID.randomUUID(),
        flightNumber = number,
        origin = IataCode.of("WAW"),
        destination = IataCode.of("LIS"),
        departureTimeZone = ZoneId.of("Europe/Warsaw"),
        departureAt = departureAt,
        arrivalAt = departureAt.plusSeconds(4 * 3600),
        aircraft = "A320neo",
        status = "SCHEDULED",
        fares = fares,
    )

    private fun useCase(vararg matches: FlightMatch): FlightSearchUseCase =
        FlightSearchUseCase(FakeFlightSearch(matches.toList()), clock)

    @Test
    fun `when a date is searched then matches come back ordered by departure then flight number`() {
        val late = match("LO100", Instant.parse("2026-11-01T18:00:00Z"))
        val earlyB = match("W61400", Instant.parse("2026-11-01T06:00:00Z"))
        val earlyA = match("LO200", Instant.parse("2026-11-01T06:00:00Z"))

        val results = useCase(late, earlyB, earlyA).search("WAW", "LIS", date, date, warsaw)

        assertEquals(listOf("LO200", "W61400", "LO100"), results.map { it.flightNumber })
    }

    @Test
    fun `when the local day is queried then the window covers midnight to midnight in that zone`() {
        val search = FakeFlightSearch(emptyList())

        FlightSearchUseCase(search, clock).search("WAW", "LIS", date, date, warsaw)

        val query = search.queries.single()
        assertEquals(Instant.parse("2026-10-31T23:00:00Z"), query.from)
        assertEquals(Instant.parse("2026-11-01T23:00:00Z"), query.until)
    }

    @Test
    fun `when the requested day has fully passed then the search is rejected`() {
        val yesterday = LocalDate.parse("2026-10-26")

        assertFailsWith<InvalidDomainValueException> {
            useCase().search("WAW", "LIS", yesterday, yesterday, warsaw)
        }
    }

    @Test
    fun `when a window is searched then the query spans the first day to the end of the last day`() {
        val search = FakeFlightSearch(emptyList())

        FlightSearchUseCase(search, clock).search("WAW", "LIS", date, date.plusDays(14), warsaw)

        val query = search.queries.single()
        assertEquals(Instant.parse("2026-10-31T23:00:00Z"), query.from)
        assertEquals(Instant.parse("2026-11-15T23:00:00Z"), query.until)
    }

    @Test
    fun `when the window ends before it starts then the search is rejected`() {
        assertFailsWith<InvalidDomainValueException> {
            useCase().search("WAW", "LIS", date, date.minusDays(1), warsaw)
        }
    }

    @Test
    fun `when the window is longer than the allowed range then the search is rejected`() {
        assertFailsWith<InvalidDomainValueException> {
            useCase().search("WAW", "LIS", date, date.plusDays(61), warsaw)
        }
    }

    @Test
    fun `when the day is today then it is still searchable even if it started before now`() {
        val today = LocalDate.parse("2026-10-27")
        val search = FakeFlightSearch(listOf(match("LO100", Instant.parse("2026-10-27T18:00:00Z"))))

        val results = FlightSearchUseCase(search, clock).search("WAW", "LIS", today, today, warsaw)

        assertEquals(1, results.size)
    }

    @Test
    fun `when origin and destination are the same then the search is rejected`() {
        assertFailsWith<InvalidDomainValueException> {
            useCase().search("WAW", "WAW", date, date, warsaw)
        }
    }

    @Test
    fun `when a cabin is requested then only that cabin is exposed and flights without it drop out`() {
        val both = match(
            "LO100",
            Instant.parse("2026-11-01T08:00:00Z"),
            listOf(
                FareOption(Cabin.ECONOMY, economy.price, 10),
                FareOption(Cabin.BUSINESS, business.price, 2),
            ),
        )
        val economyOnly = match("W61400", Instant.parse("2026-11-01T09:00:00Z"))

        val results = useCase(both, economyOnly).search("WAW", "LIS", date, date, warsaw, "business")

        assertEquals(listOf("LO100"), results.map { it.flightNumber })
        assertEquals(listOf(Cabin.BUSINESS), results.single().fares.map { it.cabin })
    }

    @Test
    fun `when no cabin is requested then every fare is returned and availability sums them`() {
        val both = match(
            "LO100",
            Instant.parse("2026-11-01T08:00:00Z"),
            listOf(
                FareOption(Cabin.ECONOMY, economy.price, 10),
                FareOption(Cabin.BUSINESS, business.price, 2),
            ),
        )

        val result = useCase(both).search("WAW", "LIS", date, date, warsaw).single()

        assertEquals(2, result.fares.size)
        assertEquals(12, result.availableSeats)
    }

    @Test
    fun `when a seat map is built then each seat is priced by its fare multiplier and ordered`() {
        val seats = listOf(
            SeatState(SeatNumber.of("2A"), business.id, SeatStatus.AVAILABLE),
            SeatState(SeatNumber.of("1A"), business.id, SeatStatus.SOLD),
            SeatState(SeatNumber.of("10C"), economy.id, SeatStatus.HELD),
        )
        val flightId = UUID.randomUUID()
        val search = FakeFlightSearch(
            matches = emptyList(),
            seatMap = SeatMapSource(
                flightId = flightId,
                flightNumber = FlightNumber.of("LO381"),
                origin = IataCode.of("WAW"),
                destination = IataCode.of("LIS"),
                departureTimeZone = ZoneId.of("Europe/Warsaw"),
                departureAt = Instant.parse("2026-11-01T08:00:00Z"),
                aircraft = "A320neo",
                status = "SCHEDULED",
                fares = listOf(economy, business),
                seats = seats,
            ),
        )

        val map = FlightSearchUseCase(search, clock).seatMap(flightId)!!

        assertEquals(listOf("1A", "2A", "10C"), map.seats.map { it.seatNumber.value })
        assertEquals(BigDecimal("1000.00"), map.seats.first().price.amount)
        assertEquals(Cabin.BUSINESS, map.seats.first().cabin)
        assertEquals(SeatStatus.SOLD, map.seats.first().status)
        assertEquals(BigDecimal("400.00"), map.seats.last().price.amount)
        assertTrue(map.seats.last().status == SeatStatus.HELD)
    }

    @Test
    fun `when the flight is unknown then the seat map lookup returns nothing`() {
        assertEquals(null, useCase().seatMap(UUID.randomUUID()))
    }

    @Test
    fun `when deals are requested then only future departures with free seats are considered`() {
        val cheap = deal("LO100", Instant.parse("2026-11-05T10:00:00Z"), "199.00", seats = 4)
        val pricier = deal("LO200", Instant.parse("2026-11-02T08:00:00Z"), "399.00", seats = 9)
        val soldOut = deal("LO300", Instant.parse("2026-11-03T08:00:00Z"), "99.00", seats = 0)
        val departed = deal("LO400", Instant.parse("2026-10-20T08:00:00Z"), "49.00", seats = 5)
        val useCase = FlightSearchUseCase(
            FakeFlightSearch(emptyList(), deals = listOf(pricier, soldOut, cheap, departed)),
            clock,
        )

        val deals = useCase.deals(limit = 6)

        assertEquals(listOf("LO100", "LO200"), deals.map { it.flightNumber })
    }

    @Test
    fun `when the cheapest flights are far in the future then they are still returned`() {
        val distant = deal("LO900", Instant.parse("2027-06-01T08:00:00Z"), "99.00", seats = 3)
        val useCase = FlightSearchUseCase(FakeFlightSearch(emptyList(), deals = listOf(distant)), clock)

        val deals = useCase.deals()

        assertEquals(listOf("LO900"), deals.map { it.flightNumber })
    }

    @Test
    fun `when deals are requested then the window starts at now with no upper bound`() {
        val search = FakeFlightSearch(emptyList())

        FlightSearchUseCase(search, clock).deals(limit = 3)

        val query = search.dealQueries.single()
        assertEquals(now, query.from)
        assertEquals(3, query.limit)
    }

    @Test
    fun `when the limit is out of range then the request is rejected`() {
        assertFailsWith<InvalidDomainValueException> {
            useCase().deals(limit = 0)
        }
        assertFailsWith<InvalidDomainValueException> {
            useCase().deals(limit = 51)
        }
    }

    private fun deal(
        number: String,
        departureAt: Instant,
        amount: String,
        seats: Int,
    ) = FlightDeal(
        flightId = UUID.randomUUID(),
        flightNumber = number,
        origin = IataCode.of("WAW"),
        destination = IataCode.of("LIS"),
        departureTimeZone = ZoneId.of("Europe/Warsaw"),
        departureAt = departureAt,
        arrivalAt = departureAt.plusSeconds(4 * 3600),
        cabin = Cabin.ECONOMY,
        price = Price(BigDecimal(amount), "PLN"),
        seatsAvailable = seats,
    )
}

private class FakeFlightSearch(
    private val matches: List<FlightMatch>,
    private val seatMap: SeatMapSource? = null,
    private val deals: List<FlightDeal> = emptyList(),
) : FlightSearch {

    val queries = mutableListOf<SearchFlightsQuery>()
    val dealQueries = mutableListOf<DealsQuery>()

    override fun search(query: SearchFlightsQuery): List<FlightMatch> {
        queries += query
        return matches
    }

    override fun seatMap(flightId: UUID): SeatMapSource? = seatMap

    override fun deals(query: DealsQuery): List<FlightDeal> {
        dealQueries += query
        return deals
    }
}
