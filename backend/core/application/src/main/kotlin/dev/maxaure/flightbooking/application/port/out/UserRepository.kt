package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId

interface UserRepository {
    fun findById(id: UserId): User?

    fun findByEmail(email: Email): User?

    fun save(user: User): User
}
