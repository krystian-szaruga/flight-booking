package dev.maxaure.flightbooking.domain.model.search

import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import dev.maxaure.flightbooking.domain.model.pricing.Price
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class FlightMatchCabinFilterTest {

    private val departure = Instant.parse("2026-11-01T08:00:00Z")
    private val query = SearchFlightsQuery(
        origin = IataCode.of("WAW"),
        destination = IataCode.of("LIS"),
        departureDate = LocalDate.parse("2026-11-01"),
        zone = ZoneId.of("Europe/Warsaw"),
    )

    private fun match(vararg fares: FareOption) = FlightMatch(
        flightId = UUID.randomUUID(),
        flightNumber = "LO381",
        origin = IataCode.of("WAW"),
        destination = IataCode.of("LIS"),
        departureAt = departure,
        arrivalAt = departure.plusSeconds(4 * 3600),
        aircraft = "A320neo",
        status = "SCHEDULED",
        fares = fares.toList(),
    )

    private val economy = FareOption(Cabin.ECONOMY, Price(BigDecimal("400.00"), "PLN"), 10)
    private val business = FareOption(Cabin.BUSINESS, Price(BigDecimal("1000.00"), "PLN"), 2)

    @Test
    fun `when a cabin is requested then only its fare survives`() {
        val filtered = query.copy(cabin = Cabin.BUSINESS)
            .matchesOf(listOf(match(economy, business)))

        assertEquals(listOf(Cabin.BUSINESS), filtered.single().fares.map { it.cabin })
    }

    @Test
    fun `when the requested cabin is not offered then the match drops out`() {
        val filtered = query.copy(cabin = Cabin.BUSINESS).matchesOf(listOf(match(economy)))

        assertEquals(emptyList(), filtered)
    }

    @Test
    fun `when no cabin is requested then every fare is kept`() {
        val kept = query.matchesOf(listOf(match(economy, business)))

        assertEquals(2, kept.single().fares.size)
    }

    @Test
    fun `when matches arrive out of order then they come back by departure then flight number`() {
        val late = match(economy).copy(flightNumber = "LO100", departureAt = departure.plusSeconds(3600))
        val earlyB = match(economy).copy(flightNumber = "W61400", departureAt = departure)
        val earlyA = match(economy).copy(flightNumber = "LO200", departureAt = departure)

        val ordered = query.matchesOf(listOf(late, earlyB, earlyA))

        assertEquals(listOf("LO200", "W61400", "LO100"), ordered.map { it.flightNumber })
    }
}
