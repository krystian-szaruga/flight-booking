package dev.maxaure.flightbooking.currency

import java.math.BigDecimal

internal data class CurrencyRates(
    val base: String,
    val values: Map<String, BigDecimal>,
) {
    companion object {
        val DEFAULT = CurrencyRates(
            base = "PLN",
            values = mapOf(
                "PLN" to BigDecimal("1.0000"),
                "EUR" to BigDecimal("0.2276"),
                "USD" to BigDecimal("0.2533"),
                "GBP" to BigDecimal("0.1902"),
                "CHF" to BigDecimal("0.2099"),
                "CZK" to BigDecimal("5.5387"),
                "DKK" to BigDecimal("1.7009"),
                "HUF" to BigDecimal("83.2878"),
                "ISK" to BigDecimal("30.9072"),
                "NOK" to BigDecimal("2.3514"),
                "RON" to BigDecimal("1.2163"),
                "SEK" to BigDecimal("2.5034"),
                "TRY" to BigDecimal("12.6244"),
                "ILS" to BigDecimal("0.7893"),
                "BGN" to BigDecimal("0.4451"),
                "BAM" to BigDecimal("0.4451"),
                "MKD" to BigDecimal("13.9970"),
                "ALL" to BigDecimal("22.3042"),
                "RSD" to BigDecimal("26.6739"),
            ),
        )
    }
}
