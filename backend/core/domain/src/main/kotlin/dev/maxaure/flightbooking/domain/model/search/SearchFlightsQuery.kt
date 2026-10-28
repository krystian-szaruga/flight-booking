package dev.maxaure.flightbooking.domain.model.search

import dev.maxaure.flightbooking.domain.model.codes.IataCode
import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SearchFlightsQuery(
    val origin: IataCode,
    val destination: IataCode,
    val departureDate: LocalDate,
    val zone: ZoneId,
    val cabin: Cabin? = null,
) {
    init {
        if (origin == destination) {
            throw InvalidDomainValueException("Origin and destination must differ, got '$origin' twice")
        }
    }

    val from: Instant get() = departureDate.atStartOfDay(zone).toInstant()

    val until: Instant get() = departureDate.plusDays(1).atStartOfDay(zone).toInstant()

    fun isDepartedBy(now: Instant): Boolean = !until.isAfter(now)

    fun matchesOf(candidates: List<FlightMatch>): List<FlightMatch> =
        candidates.mapNotNull(::inCabin).sortedWith(DEPARTURE_ORDER)

    private fun inCabin(match: FlightMatch): FlightMatch? {
        if (cabin == null) return match
        val matching = match.fares.filter { it.cabin == cabin }
        return if (matching.isEmpty()) null else match.copy(fares = matching)
    }

    private companion object {
        val DEPARTURE_ORDER: Comparator<FlightMatch> =
            compareBy({ it.departureAt }, { it.flightNumber })
    }
}
