package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.user.UpdateProfileCommand
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId

interface UpdateProfile {
    fun update(userId: UserId, command: UpdateProfileCommand): User
}
