package dev.maxaure.flightbooking.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "payment")
internal class PaymentEntity(
    @Id
    var id: UUID,

    @Column(name = "reservation_id", nullable = false)
    var reservationId: UUID,

    @Column(name = "idempotency_key", nullable = false)
    var idempotencyKey: String,

    @Column(name = "amount", nullable = false)
    var amount: BigDecimal,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    var currency: String,

    @Column(name = "status", nullable = false)
    var status: String,

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "card_last4", nullable = false, length = 4)
    var cardLast4: String,

    @Column(name = "decline_reason")
    var declineReason: String? = null,

    @Column(name = "provider_reference")
    var providerReference: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)
