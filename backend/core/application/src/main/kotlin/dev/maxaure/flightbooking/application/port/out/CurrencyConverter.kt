package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.pricing.Price

interface CurrencyConverter {

    val defaultCurrency: String

    val supportedCurrencies: List<String>

    fun convert(price: Price, target: String?): Price
}
