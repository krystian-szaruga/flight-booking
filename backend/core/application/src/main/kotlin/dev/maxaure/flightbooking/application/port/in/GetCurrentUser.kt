package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId

interface GetCurrentUser {
    fun currentUser(userId: UserId): User
}
