package dev.maxaure.flightbooking.domain.model.pricing

import dev.maxaure.flightbooking.domain.InvalidDomainValueException

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
