package dev.maxaure.flightbooking.web.auth

import dev.maxaure.flightbooking.domain.model.user.User
import java.time.Instant
import java.util.UUID

internal data class UserResponse(val id: UUID, val email: String, val createdAt: Instant) {
    companion object {
        fun from(user: User): UserResponse =
            UserResponse(id = user.id.value, email = user.email.value, createdAt = user.createdAt)
    }
}
