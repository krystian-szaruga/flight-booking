package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.user.RegisterUserCommand
import dev.maxaure.flightbooking.domain.model.user.verification.RegistrationStarted

interface RegisterUser {
    fun register(command: RegisterUserCommand): RegistrationStarted
}
