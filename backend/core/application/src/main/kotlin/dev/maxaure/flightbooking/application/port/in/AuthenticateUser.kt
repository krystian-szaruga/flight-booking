package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.user.AuthenticateUserCommand
import dev.maxaure.flightbooking.domain.model.user.User

interface AuthenticateUser {
    fun authenticate(command: AuthenticateUserCommand): User
}
