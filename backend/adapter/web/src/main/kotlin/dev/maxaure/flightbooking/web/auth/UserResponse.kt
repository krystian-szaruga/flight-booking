package dev.maxaure.flightbooking.web.auth

import dev.maxaure.flightbooking.domain.model.user.User
import java.time.Instant
import java.util.UUID

internal data class UserResponse(
    val id: UUID,
    val email: String,
    val createdAt: Instant,
    val emailVerifiedAt: Instant?,
    val firstName: String?,
    val lastName: String?,
) {
    companion object {
        fun from(user: User): UserResponse =
            UserResponse(
                id = user.id.value,
                email = user.email.value,
                createdAt = user.createdAt,
                emailVerifiedAt = user.emailVerifiedAt,
                firstName = user.firstName,
                lastName = user.lastName,
            )
    }
}
