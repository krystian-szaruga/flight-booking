package dev.maxaure.flightbooking.web.currency

import dev.maxaure.flightbooking.application.port.`in`.CurrencyOptions
import dev.maxaure.flightbooking.application.port.`in`.ListCurrencies
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/currencies")
internal class CurrencyController(
    private val currencies: ListCurrencies,
) {

    @GetMapping
    fun list(): CurrencyOptions = currencies.list()
}
