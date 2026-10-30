package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.payment.Payment
import java.util.UUID

interface PaymentRepository {

    fun findByIdempotencyKey(key: String): Payment?

    fun findLatestApproved(reservationId: UUID): Payment?

    fun save(payment: Payment): Payment
}
