package dev.maxaure.flightbooking.domain.model.payment

import dev.maxaure.flightbooking.domain.model.pricing.Price
import java.time.Instant
import java.util.UUID

enum class PaymentStatus {
    APPROVED,
    DECLINED,
}

data class Payment(
    val id: UUID,
    val reservationId: UUID,
    val idempotencyKey: String,
    val amount: Price,
    val status: PaymentStatus,
    val cardLast4: String,
    val declineReason: String?,
    val providerReference: String?,
    val createdAt: Instant,
) {
    init {
        require(idempotencyKey.isNotBlank()) { "A payment needs an idempotency key" }
        require(CARD_LAST4_PATTERN.matches(cardLast4)) { "Card last four digits must be four digits" }
        require(status == PaymentStatus.DECLINED || declineReason == null) {
            "Only a declined payment carries a decline reason"
        }
        require(status == PaymentStatus.APPROVED || providerReference == null) {
            "Only an approved payment carries a provider reference"
        }
    }

    private companion object {
        val CARD_LAST4_PATTERN = Regex("^[0-9]{4}$")
    }
}
