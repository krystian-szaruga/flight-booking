package dev.maxaure.flightbooking.currency

import dev.maxaure.flightbooking.application.port.out.CurrencyConverter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(CurrencySettings::class)
internal class CurrencyConfig {

    @Bean
    fun currencyConverter(settings: CurrencySettings): CurrencyConverter =
        StaticRateCurrencyConverter(settings.default, CurrencyRates.DEFAULT)
}
