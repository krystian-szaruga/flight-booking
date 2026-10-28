package dev.maxaure.flightbooking.domain.model.catalogue

data class AirportSummary(
    val iataCode: String,
    val name: String,
    val city: String,
    val countryCode: String,
) {
    companion object {
        val BY_CODE: Comparator<AirportSummary> = compareBy { it.iataCode }
    }
}
