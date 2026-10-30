package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.ReservationPricer
import dev.maxaure.flightbooking.domain.model.pricing.Price
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

internal class ReservationPricerAdapter(
    private val reservationSeats: SpringReservationSeatJpaRepository,
) : ReservationPricer {

    override fun priceOf(reservationId: UUID): Price? =
        reservationSeats.sumPriceForReservation(reservationId)?.let {
            val amount = it.getAmount() ?: return@let null
            val currency = it.getCurrency() ?: return@let null
            Price(amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP), currency)
        }

    private companion object {
        const val MONEY_SCALE = 2
    }
}

internal interface ReservationPriceView {
    fun getAmount(): BigDecimal?

    fun getCurrency(): String?
}
