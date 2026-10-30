package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.CurrencyOptions
import dev.maxaure.flightbooking.application.port.`in`.ListCurrencies
import dev.maxaure.flightbooking.application.port.out.CurrencyConverter

class ListCurrenciesUseCase(
    private val converter: CurrencyConverter,
) : ListCurrencies {

    override fun list(): CurrencyOptions =
        CurrencyOptions(default = converter.defaultCurrency, supported = converter.supportedCurrencies)
}
