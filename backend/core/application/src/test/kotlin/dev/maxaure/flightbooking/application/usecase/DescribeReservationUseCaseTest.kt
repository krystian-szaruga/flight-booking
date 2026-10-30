package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.PaymentRepository
import dev.maxaure.flightbooking.application.port.out.ReservationPricer
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.payment.Payment
import dev.maxaure.flightbooking.domain.model.payment.PaymentStatus
import dev.maxaure.flightbooking.domain.model.pricing.Price
import dev.maxaure.flightbooking.domain.model.reservation.PassengerSeat
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DescribeReservationUseCaseTest {

    private val now = Instant.parse("2026-10-27T10:00:00Z")
    private val price = Price(BigDecimal("399.00"), "PLN")
    private val reservation = Reservation.hold(
        flightId = UUID.randomUUID(),
        seats = listOf(PassengerSeat(SeatNumber.of("12A"), "Ada", "Lovelace")),
        now = now,
        userId = UUID.randomUUID(),
    )

    private val payments = InMemoryPaymentRepository()
    private val useCase = DescribeReservationUseCase(
        pricer = object : ReservationPricer {
            override fun priceOf(reservationId: UUID): Price? = price
        },
        payments = payments,
        converter = NoConversion,
    )

    @Test
    fun `when a reservation was paid then its description carries the price and the paid amount`() {
        payments.latest = payment(PaymentStatus.APPROVED)

        val view = useCase.describe(reservation)

        assertEquals(price, view.price)
        assertEquals(price, view.paid?.amount)
        assertEquals("4242", view.paid?.cardLast4)
    }

    @Test
    fun `when no payment was approved then the description carries the price without a paid amount`() {
        payments.latest = null

        val view = useCase.describe(reservation)

        assertEquals(price, view.price)
        assertNull(view.paid)
    }

    private fun payment(status: PaymentStatus) = Payment(
        id = UUID.randomUUID(),
        reservationId = reservation.id,
        idempotencyKey = "key-1",
        amount = price,
        status = status,
        cardLast4 = "4242",
        declineReason = null,
        providerReference = "auth-1",
        createdAt = now,
    )

    private class InMemoryPaymentRepository : PaymentRepository {
        var latest: Payment? = null

        override fun findByIdempotencyKey(key: String): Payment? = null

        override fun findLatestApproved(reservationId: UUID): Payment? =
            latest?.takeIf { it.reservationId == reservationId && it.status == PaymentStatus.APPROVED }

        override fun save(payment: Payment): Payment = payment
    }
}
