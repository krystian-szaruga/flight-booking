package dev.maxaure.flightbooking.domain.model.search

import dev.maxaure.flightbooking.domain.model.pricing.Price
import dev.maxaure.flightbooking.domain.model.pricing.Cabin

data class FareOption(
    val cabin: Cabin,
    val price: Price,
    val seatsAvailable: Int,
)
