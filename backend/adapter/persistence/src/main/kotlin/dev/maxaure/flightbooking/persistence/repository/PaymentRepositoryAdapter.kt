package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.PaymentRepository
import dev.maxaure.flightbooking.domain.model.payment.Payment
import dev.maxaure.flightbooking.persistence.mapper.PaymentMapper
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

internal class PaymentRepositoryAdapter(
    private val payments: SpringPaymentJpaRepository,
    private val transactions: TransactionTemplate,
) : PaymentRepository {

    override fun findByIdempotencyKey(key: String): Payment? =
        payments.findByIdempotencyKey(key)?.let(PaymentMapper::toDomain)

    override fun findLatestApproved(reservationId: UUID): Payment? =
        payments
            .findFirstByReservationIdAndStatusOrderByCreatedAtDesc(reservationId, "APPROVED")
            ?.let(PaymentMapper::toDomain)

    override fun save(payment: Payment): Payment = try {
        transactions.execute {
            payments.findByIdempotencyKey(payment.idempotencyKey)?.let { return@execute PaymentMapper.toDomain(it) }
            PaymentMapper.toDomain(payments.saveAndFlush(PaymentMapper.toEntity(payment)))
        }
    } catch (ex: DataIntegrityViolationException) {
        payments.findByIdempotencyKey(payment.idempotencyKey)?.let(PaymentMapper::toDomain) ?: throw ex
    }
}
