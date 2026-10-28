package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.user.PasswordHash

interface PasswordHasher {
    fun hash(rawPassword: String): PasswordHash

    fun matches(rawPassword: String, stored: PasswordHash): Boolean
}
