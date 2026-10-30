package dev.maxaure.flightbooking.web.reservation

import dev.maxaure.flightbooking.domain.model.payment.Payment
import java.math.BigDecimal
import java.time.Instant

internal data class PaymentResponse(
    val paymentId: String,
    val reservationId: String,
    val status: String,
    val amount: BigDecimal,
    val currency: String,
    val cardLast4: String,
    val declineReason: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(payment: Payment) = PaymentResponse(
            paymentId = payment.id.toString(),
            reservationId = payment.reservationId.toString(),
            status = payment.status.name,
            amount = payment.amount.amount,
            currency = payment.amount.currency,
            cardLast4 = payment.cardLast4,
            declineReason = payment.declineReason,
            createdAt = payment.createdAt,
        )
    }
}
