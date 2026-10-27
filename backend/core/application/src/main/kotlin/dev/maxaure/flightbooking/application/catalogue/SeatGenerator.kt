package dev.maxaure.flightbooking.application.catalogue

import dev.maxaure.flightbooking.domain.catalogue.model.Flight
import dev.maxaure.flightbooking.domain.catalogue.model.Seat
import dev.maxaure.flightbooking.domain.catalogue.model.SeatNumber
import java.util.UUID
import kotlin.text.iterator

internal object SeatGenerator {

    fun generate(flight: Flight): List<Seat> {
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