package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.PaymentEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface SpringPaymentJpaRepository : JpaRepository<PaymentEntity, UUID> {

    fun findByIdempotencyKey(idempotencyKey: String): PaymentEntity?
}
