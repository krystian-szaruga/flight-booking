package dev.maxaure.flightbooking.domain.model.search

import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import java.time.Instant

data class DealsQuery(
    val from: Instant,
    val limit: Int = DEFAULT_LIMIT,
) {
    init {
        if (limit !in 1..MAX_LIMIT) {
            throw InvalidDomainValueException("Limit must be between 1 and $MAX_LIMIT, got $limit")
        }
    }

    fun offersOf(candidates: List<FlightDeal>): List<FlightDeal> =
        candidates
            .filter { it.seatsAvailable > 0 && it.departureAt.isAfter(from) }
            .sortedWith(CHEAPEST_FIRST)
            .take(limit)

    private companion object {
        const val DEFAULT_LIMIT = 6
        const val MAX_LIMIT = 50

        val CHEAPEST_FIRST: Comparator<FlightDeal> =
            compareBy({ it.price.amount }, { it.price.currency }, { it.departureAt })
    }
}
