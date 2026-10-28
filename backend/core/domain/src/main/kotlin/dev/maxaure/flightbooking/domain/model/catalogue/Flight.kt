package dev.maxaure.flightbooking.domain.model.catalogue

import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.model.pricing.FareClass
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.seat.Seat
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import java.time.Instant
import java.util.UUID

data class Flight(
    val id: UUID,
    val flightNumber: FlightNumber,
    val origin: Airport,
    val destination: Airport,
    val departureAt: Instant,
    val arrivalAt: Instant,
    val aircraft: String,
    val status: FlightStatus,
    val fares: List<FareClass>,
    val seatRows: Int = DEFAULT_SEAT_ROWS,
    val source: String = MANUAL_SOURCE,
    val syncedAt: Instant? = null
) {
    init {
        require(arrivalAt.isAfter(departureAt)) { "Arrival must be after departure" }
        if (origin.id == destination.id) {
            throw InvalidDomainValueException("Origin and destination must differ")
        }
        require(aircraft.isNotBlank()) { "Aircraft must not be blank" }
        require(fares.isNotEmpty()) { "A flight must offer at least one fare class" }
        require(fares.map { it.cabin }.toSet().size == fares.size) { "Cabin may only appear once per flight" }
        require(seatRows in 1..MAX_SEAT_ROWS) { "Seat rows must be between 1 and $MAX_SEAT_ROWS" }
        require(source.isNotBlank()) { "Source must not be blank" }
    }

    val identity: FlightIdentity get() = FlightIdentity(flightNumber, departureAt)

    fun seatInventory(): List<Seat> = SeatLayout(this).inventory()

    private class SeatLayout(private val flight: Flight) {

        fun inventory(): List<Seat> {
            val cabins = flight.fares.sortedByDescending { it.cabin.ordinal }
            if (cabins.size == 1) {
                return seatsFor(rows = 1..flight.seatRows, fareClassId = cabins.single().id)
            }

            val rowsPerCabin = flight.seatRows / cabins.size
            require(rowsPerCabin >= 1) {
                "Flight needs at least ${cabins.size} seat rows for ${cabins.size} cabins, has ${flight.seatRows}"
            }

            var nextRow = 1
            return buildList {
                cabins.forEachIndexed { index, fare ->
                    val isLast = index == cabins.lastIndex
                    val rowCount = if (isLast) flight.seatRows - nextRow + 1 else rowsPerCabin
                    addAll(seatsFor(rows = nextRow until nextRow + rowCount, fareClassId = fare.id))
                    nextRow += rowCount
                }
            }
        }

        private fun seatsFor(rows: IntRange, fareClassId: UUID): List<Seat> = buildList {
            for (row in rows) {
                for (letter in SeatNumber.SEAT_LETTERS) {
                    add(Seat(seatNumber = SeatNumber.of(row, letter), fareClassId = fareClassId))
                }
            }
        }
    }

    companion object {
        const val DEFAULT_SEAT_ROWS = 30
        const val MAX_SEAT_ROWS = 60
        const val MANUAL_SOURCE = "manual"
    }
}
