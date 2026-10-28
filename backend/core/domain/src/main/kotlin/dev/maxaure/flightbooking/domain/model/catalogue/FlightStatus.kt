package dev.maxaure.flightbooking.domain.model.catalogue

import dev.maxaure.flightbooking.domain.InvalidDomainValueException

enum class FlightStatus {
    SCHEDULED,
    DELAYED,
    CANCELLED;

    companion object {
        fun parse(raw: String): FlightStatus = entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
            ?: throw InvalidDomainValueException("Unknown flight status '$raw'")
    }
}
