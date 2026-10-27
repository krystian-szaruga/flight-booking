package dev.maxaure.flightbooking.domain.catalogue.model

import java.util.UUID

data class Seat(
    val seatNumber: SeatNumber,
    val fareClassId: UUID,
    val status: SeatStatus = SeatStatus.AVAILABLE,
    val version: Long = 0,
)

enum class SeatStatus { AVAILABLE, HELD, SOLD }

@JvmInline
value class SeatNumber private constructor(val value: String) {
    val row: Int get() = value.dropLast(1).toInt()

    override fun toString(): String = value

    companion object {
        private val PATTERN = Regex("^[0-9]{1,2}[A-F]$")

        fun of(row: Int, letter: Char): SeatNumber {
            require(row in 1..MAX_ROW) { "Seat row must be between 1 and $MAX_ROW, got $row" }
            val normalised = letter.uppercaseChar()
            require(normalised in SEAT_LETTERS) { "Seat letter must be one of $SEAT_LETTERS, got '$letter'" }
            return SeatNumber("$row$normalised")
        }

        fun of(raw: String): SeatNumber {
            val normalised = raw.trim().uppercase()
            require(PATTERN.matches(normalised)) { "Seat number must look like 12C, got '$raw'" }
            require(normalised.dropLast(1).toInt() in 1..MAX_ROW) { "Seat row out of range in '$raw'" }
            return SeatNumber(normalised)
        }

        fun fromTrusted(value: String): SeatNumber = SeatNumber(value)

        const val MAX_ROW = 99
        const val SEAT_LETTERS = "ABCDEF"
    }
}
