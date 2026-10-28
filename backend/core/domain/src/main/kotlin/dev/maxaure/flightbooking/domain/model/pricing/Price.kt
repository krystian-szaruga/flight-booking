package dev.maxaure.flightbooking.domain.model.pricing

import java.math.BigDecimal

data class Price(val amount: BigDecimal, val currency: String) {
    init {
        require(amount.scale() <= 2) { "Price amount must have at most 2 decimal places" }
        require(amount > BigDecimal.ZERO) { "Price amount must be positive" }
        require(CURRENCY_PATTERN.matches(currency)) { "Currency must be an ISO-4217 code, got '$currency'" }
    }

    private companion object {
        val CURRENCY_PATTERN = Regex("^[A-Z]{3}$")
    }
}
