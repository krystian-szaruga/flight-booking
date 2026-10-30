package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.CurrencyConverter
import dev.maxaure.flightbooking.domain.model.pricing.Price

internal object NoConversion : CurrencyConverter {
    override val defaultCurrency: String = "PLN"
    override val supportedCurrencies: List<String> = listOf("PLN")

    override fun convert(price: Price, target: String?): Price = price
}
