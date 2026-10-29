package dev.maxaure.flightbooking.domain.model.user

import java.time.Instant
import java.util.UUID

data class RegisterUserCommand(val email: String, val rawPassword: String)

data class AuthenticateUserCommand(
    val email: String,
    val rawPassword: String,
    val address: String,
)

data class RevokeTokenCommand(val tokenId: UUID, val expiresAt: Instant)
