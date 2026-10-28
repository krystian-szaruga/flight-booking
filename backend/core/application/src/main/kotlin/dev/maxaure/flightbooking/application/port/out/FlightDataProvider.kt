package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.catalogue.FeedPage
import java.time.LocalDate

interface FlightDataProvider {

    val id: String

    fun fetch(query: FeedRequest): FeedPage
}

data class FeedRequest(
    val from: LocalDate,
    val to: LocalDate,
) {
    init { require(!to.isBefore(from)) { "'to' must not be before 'from'" } }
}
