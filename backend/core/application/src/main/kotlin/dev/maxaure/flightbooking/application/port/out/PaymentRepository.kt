package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.payment.Payment

interface PaymentRepository {

    fun findByIdempotencyKey(key: String): Payment?

    fun save(payment: Payment): Payment
}
