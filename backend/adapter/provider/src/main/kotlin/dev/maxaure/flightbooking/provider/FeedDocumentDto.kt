package dev.maxaure.flightbooking.provider

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant

internal data class FeedDocumentDto @JsonCreator constructor(
    @JsonProperty("airports") val airports: List<FeedAirportDto> = emptyList(),
    @JsonProperty("flights") val flights: List<FeedFlightDto> = emptyList(),
)

internal data class FeedAirportDto @JsonCreator constructor(
    @JsonProperty("iataCode") val iataCode: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("city") val city: String,
    @JsonProperty("countryCode") val countryCode: String,
)

internal data class FeedFlightDto @JsonCreator constructor(
    @JsonProperty("flightNumber") val flightNumber: String,
    @JsonProperty("origin") val origin: String,
    @JsonProperty("destination") val destination: String,
    @JsonProperty("departureAt") val departureAt: Instant,
    @JsonProperty("arrivalAt") val arrivalAt: Instant,
    @JsonProperty("aircraft") val aircraft: String,
    @JsonProperty("fares") val fares: List<FeedFareDto>,
    @JsonProperty("seatRows") val seatRows: Int? = null,
    @JsonProperty("status") val status: String? = null,
)

internal data class FeedFareDto @JsonCreator constructor(
    @JsonProperty("cabin") val cabin: String,
    @JsonProperty("amount") val amount: BigDecimal,
    @JsonProperty("currency") val currency: String,
    @JsonProperty("multiplier") val multiplier: BigDecimal? = null,
)
