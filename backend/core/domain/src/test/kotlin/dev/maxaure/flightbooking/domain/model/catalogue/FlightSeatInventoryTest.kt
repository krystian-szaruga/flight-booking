package dev.maxaure.flightbooking.domain.model.catalogue

import dev.maxaure.flightbooking.domain.model.pricing.FareClass
import dev.maxaure.flightbooking.domain.model.pricing.Price
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FlightSeatInventoryTest {

    private fun flight(cabins: List<Cabin>, rows: Int): Flight {
        val fares = cabins.map { FareClass(UUID.randomUUID(), it, Price(BigDecimal("100.00"), "PLN")) }
        val start = Instant.parse("2026-11-01T08:00:00Z")
        return Flight(
            id = UUID.randomUUID(),
            flightNumber = FlightNumber.of("LO381"),
            origin = Airport.create("WAW", "Warsaw Chopin", "Warsaw", "PL"),
            destination = Airport.create("LIS", "Lisbon", "Lisbon", "PT"),
            departureAt = start,
            arrivalAt = start.plusSeconds(4 * 3600),
            aircraft = "A320neo",
            status = FlightStatus.SCHEDULED,
            fares = fares,
            seatRows = rows,
        )
    }

    @Test
    fun `when flight offers two cabins then business rows come first and economy takes the remainder`() {
        val flight = flight(listOf(Cabin.ECONOMY, Cabin.BUSINESS), rows = 7)

        val seats = flight.seatInventory()

        val business = flight.fares.first { it.cabin == Cabin.BUSINESS }
        val economy = flight.fares.first { it.cabin == Cabin.ECONOMY }
        assertTrue(seats.filter { it.fareClassId == business.id }.all { it.seatNumber.row in 1..3 })
        assertTrue(seats.filter { it.fareClassId == economy.id }.all { it.seatNumber.row in 4..7 })
        assertEquals(42, seats.size)
    }

    @Test
    fun `when rows do not divide evenly across three cabins then no row is dropped`() {
        val flight = flight(listOf(Cabin.FIRST, Cabin.BUSINESS, Cabin.ECONOMY), rows = 10)

        val seats = flight.seatInventory()

        assertEquals(60, seats.size)
        assertEquals((1..10).toSet(), seats.map { it.seatNumber.row }.toSet())
        val perCabin = seats.groupBy { it.fareClassId }.mapValues { it.value.map { s -> s.seatNumber.row }.distinct().size }
        assertEquals(3, perCabin.getValue(flight.fares.first { it.cabin == Cabin.FIRST }.id))
        assertEquals(3, perCabin.getValue(flight.fares.first { it.cabin == Cabin.BUSINESS }.id))
        assertEquals(4, perCabin.getValue(flight.fares.first { it.cabin == Cabin.ECONOMY }.id))
    }

    @Test
    fun `when every row is allocated then each seat points at a fare class of this flight`() {
        val flight = flight(listOf(Cabin.BUSINESS, Cabin.PREMIUM_ECONOMY, Cabin.ECONOMY), rows = 11)

        val seats = flight.seatInventory()

        val fareIds = flight.fares.map { it.id }.toSet()
        assertEquals(66, seats.size)
        assertTrue(seats.all { it.fareClassId in fareIds })
    }

    @Test
    fun `when there are more cabins than rows then generation fails instead of dropping a cabin`() {
        assertFailsWith<IllegalArgumentException> {
            flight(listOf(Cabin.FIRST, Cabin.BUSINESS, Cabin.ECONOMY), rows = 2).seatInventory()
        }
    }
}
