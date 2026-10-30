package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.PayReservation
import dev.maxaure.flightbooking.application.port.`in`.PaymentRequest
import dev.maxaure.flightbooking.application.port.out.CardDetails
import dev.maxaure.flightbooking.application.port.out.CurrencyConverter
import dev.maxaure.flightbooking.application.port.out.PaymentCommand
import dev.maxaure.flightbooking.application.port.out.PaymentGateway
import dev.maxaure.flightbooking.application.port.out.PaymentRepository
import dev.maxaure.flightbooking.application.port.out.PaymentResult
import dev.maxaure.flightbooking.application.port.out.ReservationPricer
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.domain.ConflictException
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.payment.Payment
import dev.maxaure.flightbooking.domain.model.payment.PaymentStatus
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Clock
import java.util.UUID

class PayReservationUseCase(
    private val reservations: ReservationRepository,
    private val payments: PaymentRepository,
    private val pricer: ReservationPricer,
    private val gateway: PaymentGateway,
    private val converter: CurrencyConverter,
    private val clock: Clock,
) : PayReservation {

    override fun pay(userId: UserId, reservationId: UUID, request: PaymentRequest): Payment {
        val reservation = ownedOrMissing(userId, reservationId)
        val replay = payments.findByIdempotencyKey(request.idempotencyKey)
        if (replay != null) return replayFor(replay, reservationId)

        val now = clock.instant()
        reservation.pay(now)
        val amount = pricer.priceOf(reservationId)
            ?.let { converter.convert(it, request.currency) }
            ?: throw NotFoundException("Reservation $reservationId has no priced seats")
        val command = PaymentCommand(
            idempotencyKey = request.idempotencyKey,
            reservationId = reservationId,
            amount = amount,
            card = CardDetails.of(request.cardNumber, request.cardHolder),
        )
        val payment = payments.save(
            when (val result = gateway.authorize(command)) {
                is PaymentResult.Approved -> payment(command, PaymentStatus.APPROVED, result.reference, null, now)
                is PaymentResult.Declined -> payment(command, PaymentStatus.DECLINED, null, result.reason, now)
            },
        )
        if (payment.status == PaymentStatus.APPROVED) {
            reservations.confirm(reservationId)
                ?: throw NotFoundException("Reservation $reservationId does not exist")
        }
        return payment
    }

    private fun replayFor(payment: Payment, reservationId: UUID): Payment {
        if (payment.reservationId != reservationId) {
            throw ConflictException("Idempotency key ${payment.idempotencyKey} belongs to another reservation")
        }
        return payment
    }

    private fun payment(
        command: PaymentCommand,
        status: PaymentStatus,
        reference: String?,
        declineReason: String?,
        now: java.time.Instant,
    ): Payment = Payment(
        id = UUID.randomUUID(),
        reservationId = command.reservationId,
        idempotencyKey = command.idempotencyKey,
        amount = command.amount,
        status = status,
        cardLast4 = command.card.last4,
        declineReason = declineReason,
        providerReference = reference,
        createdAt = now,
    )

    private fun ownedOrMissing(userId: UserId, reservationId: UUID): Reservation {
        val reservation = reservations.findById(reservationId)
            ?: throw NotFoundException("Reservation $reservationId does not exist")
        if (reservation.userId != userId.value) throw NotFoundException("Reservation $reservationId does not exist")
        return reservation
    }
}
