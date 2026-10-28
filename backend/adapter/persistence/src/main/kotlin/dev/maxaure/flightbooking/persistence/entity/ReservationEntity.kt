package dev.maxaure.flightbooking.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "reservation")
internal class ReservationEntity(
    @Id
    var id: UUID,

    @Column(name = "flight_id", nullable = false)
    var flightId: UUID,

    @Column(name = "user_id")
    var userId: UUID? = null,

    @Column(name = "status", nullable = false)
    var status: String,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)
