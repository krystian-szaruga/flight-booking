package dev.maxaure.flightbooking.domain.model.codes

import dev.maxaure.flightbooking.domain.InvalidDomainValueException

@JvmInline
value class IataCode private constructor(val value: String) : Comparable<IataCode> {
    override fun toString(): String = value

    override fun compareTo(other: IataCode): Int = value.compareTo(other.value)

    companion object {
        private val PATTERN = Regex("^[A-Za-z]{3}$")

        fun of(raw: String): IataCode {
            val normalised = raw.trim().uppercase()
            if (!PATTERN.matches(normalised)) {
                throw InvalidDomainValueException("IATA code must be exactly three letters, got '$raw'")
            }
            return IataCode(normalised)
        }

        fun fromTrusted(value: String): IataCode = IataCode(value)
    }
}

@JvmInline
value class CountryCode private constructor(val value: String) {
    override fun toString(): String = value

    companion object {
        private val PATTERN = Regex("^[A-Za-z]{2}$")

        fun of(raw: String): CountryCode {
            val normalised = raw.trim().uppercase()
            if (!PATTERN.matches(normalised)) {
                throw InvalidDomainValueException("Country code must be exactly two letters, got '$raw'")
            }
            return CountryCode(normalised)
        }

        fun fromTrusted(value: String): CountryCode = CountryCode(value)
    }
}

@JvmInline
value class FlightNumber private constructor(val value: String) : Comparable<FlightNumber> {
    override fun toString(): String = value

    override fun compareTo(other: FlightNumber): Int = value.compareTo(other.value)

    companion object {
        private val PATTERN = Regex("^[A-Z]{1,3}[0-9]{1,5}$")

        fun of(raw: String): FlightNumber {
            val normalised = raw.trim().uppercase().replace(" ", "")
            if (!PATTERN.matches(normalised)) {
                throw InvalidDomainValueException(
                    "Flight number must be 1-3 letters followed by 1-5 digits, got '$raw'",
                )
            }
            return FlightNumber(normalised)
        }

        fun fromTrusted(value: String): FlightNumber = FlightNumber(value)
    }
}

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
