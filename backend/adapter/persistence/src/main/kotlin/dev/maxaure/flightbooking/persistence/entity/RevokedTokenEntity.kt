package dev.maxaure.flightbooking.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "revoked_token")
internal class RevokedTokenEntity(
    @Id
    @Column(name = "token_id")
    var tokenId: UUID,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
)
