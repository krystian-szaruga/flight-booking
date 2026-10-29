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
    val departureFrom: LocalDate,
    val departureTo: LocalDate,
    val zone: ZoneId,
    val cabin: Cabin? = null,
) {
    init {
        if (origin == destination) {
            throw InvalidDomainValueException("Origin and destination must differ, got '$origin' twice")
        }
        if (departureTo.isBefore(departureFrom)) {
            throw InvalidDomainValueException("Departure window must end on or after it starts")
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(departureFrom, departureTo) > MAX_WINDOW_DAYS) {
            throw InvalidDomainValueException("Departure window must be at most $MAX_WINDOW_DAYS days")
        }
    }

    val from: Instant get() = departureFrom.atStartOfDay(zone).toInstant()

    val until: Instant get() = departureTo.plusDays(1).atStartOfDay(zone).toInstant()

    fun isDepartedBy(now: Instant): Boolean = !until.isAfter(now)

    fun matchesOf(candidates: List<FlightMatch>): List<FlightMatch> =
        candidates.mapNotNull(::inCabin).sortedWith(DEPARTURE_ORDER)

    private fun inCabin(match: FlightMatch): FlightMatch? {
        if (cabin == null) return match
        val matching = match.fares.filter { it.cabin == cabin }
        return if (matching.isEmpty()) null else match.copy(fares = matching)
    }

    private companion object {
        const val MAX_WINDOW_DAYS = 60L

        val DEPARTURE_ORDER: Comparator<FlightMatch> =
            compareBy({ it.departureAt }, { it.flightNumber })
    }
}
