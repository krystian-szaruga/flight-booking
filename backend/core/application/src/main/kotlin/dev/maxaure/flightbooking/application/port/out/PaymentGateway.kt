package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.model.pricing.Price
import java.util.UUID

interface PaymentGateway {

    fun authorize(command: PaymentCommand): PaymentResult
}

data class PaymentCommand(
    val idempotencyKey: String,
    val reservationId: UUID,
    val amount: Price,
    val card: CardDetails,
)

data class CardDetails(
    val number: String,
    val holder: String,
) {
    val last4: String get() = number.takeLast(4)

    companion object {
        private val DIGITS_PATTERN = Regex("^[0-9]+$")

        fun of(number: String, holder: String): CardDetails =
            CardDetails(number.filterNot(Char::isWhitespace), holder.trim())
    }

    init {
        if (!DIGITS_PATTERN.matches(number) || number.length !in 12..19) {
            throw InvalidDomainValueException("Card number must contain between 12 and 19 digits")
        }
        if (holder.isBlank()) throw InvalidDomainValueException("Card holder must not be blank")
    }
}

sealed interface PaymentResult {
    data class Approved(val reference: String) : PaymentResult

    data class Declined(val reason: String) : PaymentResult
}
