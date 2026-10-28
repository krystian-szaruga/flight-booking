package dev.maxaure.flightbooking.domain.model.catalogue

data class AirportSearchQuery private constructor(
    val text: String,
    val limit: Int,
) {
    val isEmpty: Boolean get() = text.isEmpty()

    companion object {
        const val MAX_LIMIT = 25

        fun of(rawQuery: String, limit: Int): AirportSearchQuery = AirportSearchQuery(
            text = rawQuery.trim(),
            limit = limit.coerceIn(1, MAX_LIMIT),
        )
    }
}
