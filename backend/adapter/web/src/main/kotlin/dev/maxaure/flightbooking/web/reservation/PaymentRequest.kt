package dev.maxaure.flightbooking.web.reservation

import dev.maxaure.flightbooking.application.port.`in`.PaymentRequest
import jakarta.validation.constraints.NotBlank

internal data class PayReservationRequest(
    @field:NotBlank(message = "An idempotency key is required")
    val idempotencyKey: String,

    @field:NotBlank(message = "A card number is required")
    val cardNumber: String,

    @field:NotBlank(message = "A card holder is required")
    val cardHolder: String,

    val currency: String? = null,
) {
    fun toRequest(): PaymentRequest = PaymentRequest(
        idempotencyKey = idempotencyKey,
        cardNumber = cardNumber,
        cardHolder = cardHolder,
        currency = currency,
    )
}
