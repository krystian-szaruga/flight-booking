package dev.maxaure.flightbooking.domain.model.pricing

import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

data class FareClass(
    val id: UUID,
    val cabin: Cabin,
    val price: Price,
    val multiplier: BigDecimal = BigDecimal.ONE,
) {
    init {
        require(multiplier > BigDecimal.ZERO) { "Fare multiplier must be positive" }
    }

    fun priceForSeat(): Price = Price(
        price.amount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP),
        price.currency,
    )
}
