package dev.maxaure.flightbooking.payment

import dev.maxaure.flightbooking.application.port.out.PaymentCommand
import dev.maxaure.flightbooking.application.port.out.PaymentGateway
import dev.maxaure.flightbooking.application.port.out.PaymentResult
import java.util.UUID

internal class FakePaymentGateway : PaymentGateway {

    override fun authorize(command: PaymentCommand): PaymentResult = when (command.card.number) {
        APPROVES -> PaymentResult.Approved("fake-${UUID.randomUUID()}")
        DECLINES -> PaymentResult.Declined("Card declined by issuer")
        INSUFFICIENT_FUNDS -> PaymentResult.Declined("Insufficient funds")
        else -> PaymentResult.Declined("Unsupported test card")
    }

    private companion object {
        const val APPROVES = "4242424242424242"
        const val DECLINES = "4000000000000002"
        const val INSUFFICIENT_FUNDS = "4000000000009995"
    }
}
