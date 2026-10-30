package dev.maxaure.flightbooking.currency

import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.model.pricing.Price
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StaticRateCurrencyConverterTest {

    private fun converter(
        base: String = "PLN",
        default: String = "PLN",
        rates: Map<String, BigDecimal> = mapOf(
            "PLN" to BigDecimal("1.00"),
            "EUR" to BigDecimal("0.23"),
            "USD" to BigDecimal("0.25"),
        ),
    ) = StaticRateCurrencyConverter(default, CurrencyRates(base, rates))

    @Test
    fun `when a price is converted then the target rate is applied and rounded half up`() {
        val converted = converter().convert(Price(BigDecimal("101.05"), "PLN"), "EUR")

        assertEquals(Price(BigDecimal("23.24"), "EUR"), converted)
    }

    @Test
    fun `when the target is the source currency then the price is unchanged`() {
        val price = Price(BigDecimal("399.00"), "PLN")

        assertEquals(price, converter().convert(price, "PLN"))
    }

    @Test
    fun `when no target is requested then the price is unchanged`() {
        val price = Price(BigDecimal("399.00"), "EUR")

        assertEquals(price, converter().convert(price, null))
    }

    @Test
    fun `when the rate is quoted in another base then conversion goes through the base`() {
        val converted = converter().convert(Price(BigDecimal("25.00"), "USD"), "EUR")

        assertEquals(Price(BigDecimal("23.00"), "EUR"), converted)
    }

    @Test
    fun `when the currency has no rate then the conversion is rejected`() {
        assertFailsWith<InvalidDomainValueException> {
            converter().convert(Price(BigDecimal("10.00"), "PLN"), "CHF")
        }
    }

    @Test
    fun `when the source currency has no rate then the price is returned unchanged`() {
        val price = Price(BigDecimal("180.00"), "BAM")

        assertEquals(price, converter().convert(price, "PLN"))
    }

    @Test
    fun `when the default currency has no rate then the adapter refuses to start`() {
        assertFailsWith<IllegalArgumentException> {
            converter(default = "GBP")
        }
    }

    @Test
    fun `when the default table is used then every feed currency converts to the selected one`() {
        val loaded = StaticRateCurrencyConverter("PLN", CurrencyRates.DEFAULT)

        assertTrue(loaded.supportedCurrencies.containsAll(listOf("PLN", "EUR", "BAM", "BGN", "HUF", "ALL")))
        assertEquals("PLN", loaded.convert(Price(BigDecimal("100.00"), "BAM"), "PLN").currency)
        assertEquals("EUR", loaded.convert(Price(BigDecimal("100.00"), "ALL"), "EUR").currency)
    }
}
