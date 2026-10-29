package dev.maxaure.flightbooking.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "pending_registration")
internal class PendingRegistrationEntity(
    @Id
    @Column(name = "email", nullable = false)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "code_hash", nullable = false)
    var codeHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "attempts", nullable = false)
    var attempts: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "sent_at", nullable = false)
    var sentAt: Instant,

    @Column(name = "send_count", nullable = false)
    var sendCount: Int,
)
