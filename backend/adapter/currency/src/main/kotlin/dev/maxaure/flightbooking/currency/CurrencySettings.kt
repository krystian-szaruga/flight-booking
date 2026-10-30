package dev.maxaure.flightbooking.currency

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.currency")
internal data class CurrencySettings(
    val default: String = "PLN",
)
