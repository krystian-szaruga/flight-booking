package dev.maxaure.flightbooking.persistence.mapper

import dev.maxaure.flightbooking.domain.model.payment.Payment
import dev.maxaure.flightbooking.domain.model.payment.PaymentStatus
import dev.maxaure.flightbooking.domain.model.pricing.Price
import dev.maxaure.flightbooking.persistence.entity.PaymentEntity

internal object PaymentMapper {

    fun toDomain(entity: PaymentEntity): Payment = Payment(
        id = entity.id,
        reservationId = entity.reservationId,
        idempotencyKey = entity.idempotencyKey,
        amount = Price(entity.amount, entity.currency),
        status = PaymentStatus.valueOf(entity.status),
        cardLast4 = entity.cardLast4,
        declineReason = entity.declineReason,
        providerReference = entity.providerReference,
        createdAt = entity.createdAt,
    )

    fun toEntity(payment: Payment): PaymentEntity = PaymentEntity(
        id = payment.id,
        reservationId = payment.reservationId,
        idempotencyKey = payment.idempotencyKey,
        amount = payment.amount.amount,
        currency = payment.amount.currency,
        status = payment.status.name,
        cardLast4 = payment.cardLast4,
        declineReason = payment.declineReason,
        providerReference = payment.providerReference,
        createdAt = payment.createdAt,
    )
}
