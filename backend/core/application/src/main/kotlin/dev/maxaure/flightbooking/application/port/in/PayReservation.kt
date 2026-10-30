package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.payment.Payment
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.util.UUID

data class PaymentRequest(
    val idempotencyKey: String,
    val cardNumber: String,
    val cardHolder: String,
    val currency: String? = null,
)

interface PayReservation {

    fun pay(userId: UserId, reservationId: UUID, request: PaymentRequest): Payment
}
