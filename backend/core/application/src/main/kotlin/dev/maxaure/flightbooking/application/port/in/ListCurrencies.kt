package dev.maxaure.flightbooking.application.port.`in`

data class CurrencyOptions(
    val default: String,
    val supported: List<String>,
)

interface ListCurrencies {

    fun list(): CurrencyOptions
}
