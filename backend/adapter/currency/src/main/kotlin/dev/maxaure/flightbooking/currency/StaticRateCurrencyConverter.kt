package dev.maxaure.flightbooking.currency

import dev.maxaure.flightbooking.application.port.out.CurrencyConverter
import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.model.pricing.Price
import java.math.BigDecimal
import java.math.RoundingMode

internal class StaticRateCurrencyConverter(
    default: String,
    currencyRates: CurrencyRates,
) : CurrencyConverter {

    private val rates: Map<String, BigDecimal> = currencyRates.values.mapKeys { it.key.uppercase() }

    override val defaultCurrency: String = default.uppercase()

    override val supportedCurrencies: List<String> = rates.keys.sorted()

    init {
        require(rates[currencyRates.base.uppercase()]?.compareTo(BigDecimal.ONE) == 0) {
            "Currency rate for base ${currencyRates.base} must be 1.00"
        }
        require(defaultCurrency in rates) { "Default currency $defaultCurrency has no rate" }
    }

    override fun convert(price: Price, target: String?): Price {
        val currency = target?.trim()?.uppercase().orEmpty()
        if (currency.isEmpty() || currency == price.currency) return price
        val targetRate = rates[currency] ?: throw InvalidDomainValueException("Unsupported currency '$currency'")
        val sourceRate = rates[price.currency] ?: return price
        val amount = price.amount
            .multiply(targetRate)
            .divide(sourceRate, MONEY_SCALE, RoundingMode.HALF_UP)
        return Price(amount, currency)
    }

    private companion object {
        const val MONEY_SCALE = 2
    }
}
