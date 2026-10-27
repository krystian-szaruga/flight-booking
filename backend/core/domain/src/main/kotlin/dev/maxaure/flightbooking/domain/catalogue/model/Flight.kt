package dev.maxaure.flightbooking.domain.catalogue.model

import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class FlightStatus {
    SCHEDULED,
    DELAYED,
    CANCELLED;

    companion object {
        fun parse(raw: String): FlightStatus = entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
            ?: throw InvalidDomainValueException("Unknown flight status '$raw'")
    }
}

enum class Cabin {
    ECONOMY,
    PREMIUM_ECONOMY,
    BUSINESS,
    FIRST;

    companion object {
        fun parse(raw: String): Cabin {
            val normalised = raw.trim().uppercase().replace('-', '_').replace(' ', '_')
            return entries.firstOrNull { it.name == normalised }
                ?: throw InvalidDomainValueException("Unknown cabin '$raw'")
        }
    }
}

data class Price(val amount: BigDecimal, val currency: String) {
    init {
        require(amount.scale() <= 2) { "Price amount must have at most 2 decimal places" }
        require(amount > BigDecimal.ZERO) { "Price amount must be positive" }
        require(CURRENCY_PATTERN.matches(currency)) { "Currency must be an ISO-4217 code, got '$currency'" }
    }

    private companion object {
        val CURRENCY_PATTERN = Regex("^[A-Z]{3}$")
    }
}

data class FareClass(
    val id: UUID,
    val cabin: Cabin,
    val price: Price,
    val multiplier: BigDecimal = BigDecimal.ONE,
) {
    init {
        require(multiplier > BigDecimal.ZERO) { "Fare multiplier must be positive" }
    }
}

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
        require(origin.id != destination.id) { "Origin and destination must differ" }
        require(aircraft.isNotBlank()) { "Aircraft must not be blank" }
        require(fares.isNotEmpty()) { "A flight must offer at least one fare class" }
        require(fares.map { it.cabin }.toSet().size == fares.size) { "Cabin may only appear once per flight" }
        require(seatRows in 1..MAX_SEAT_ROWS) { "Seat rows must be between 1 and $MAX_SEAT_ROWS" }
        require(source.isNotBlank()) { "Source must not be blank" }
    }

    val identity: FlightIdentity get() = FlightIdentity(flightNumber, departureAt)

    companion object {
        const val DEFAULT_SEAT_ROWS = 30
        const val MAX_SEAT_ROWS = 60
        const val MANUAL_SOURCE = "manual"
    }
}

data class FlightIdentity(val flightNumber: FlightNumber, val departureAt: Instant)