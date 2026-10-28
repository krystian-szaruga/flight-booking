package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Instant
import java.util.UUID

data class AccessToken(val userId: UserId, val id: UUID, val expiresAt: Instant)

interface AccessTokenIssuer {
    fun issue(userId: UserId): String

    fun parse(token: String): AccessToken?
}
