package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.PaymentRepository
import dev.maxaure.flightbooking.domain.model.payment.Payment
import dev.maxaure.flightbooking.persistence.mapper.PaymentMapper
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.support.TransactionTemplate

internal class PaymentRepositoryAdapter(
    private val payments: SpringPaymentJpaRepository,
    private val transactions: TransactionTemplate,
) : PaymentRepository {

    override fun findByIdempotencyKey(key: String): Payment? =
        payments.findByIdempotencyKey(key)?.let(PaymentMapper::toDomain)

    override fun save(payment: Payment): Payment = try {
        transactions.execute {
            payments.findByIdempotencyKey(payment.idempotencyKey)?.let { return@execute PaymentMapper.toDomain(it) }
            PaymentMapper.toDomain(payments.saveAndFlush(PaymentMapper.toEntity(payment)))
        } ?: error("Payment transaction returned no result")
    } catch (ex: DataIntegrityViolationException) {
        payments.findByIdempotencyKey(payment.idempotencyKey)?.let(PaymentMapper::toDomain) ?: throw ex
    }
}
