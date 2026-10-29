package dev.maxaure.flightbooking.application.usecase.user

import dev.maxaure.flightbooking.application.port.`in`.AuthenticateUser
import dev.maxaure.flightbooking.application.port.out.LoginAttemptKey
import dev.maxaure.flightbooking.application.port.out.LoginAttemptLimiter
import dev.maxaure.flightbooking.application.port.out.PasswordHasher
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.domain.model.user.AuthenticateUserCommand
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.InvalidCredentials
import dev.maxaure.flightbooking.domain.model.user.User

class AuthenticateUserUseCase(
    private val users: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val loginAttempts: LoginAttemptLimiter,
) : AuthenticateUser {

    override fun authenticate(command: AuthenticateUserCommand): User {
        val email = runCatching { Email.of(command.email) }.getOrNull()
        val key = LoginAttemptKey(command.address, email?.value ?: command.email.trim().lowercase())
        loginAttempts.check(key)

        val user = email?.let { users.findByEmail(it) } ?: run {
            loginAttempts.recordFailure(key)
            throw InvalidCredentials()
        }
        if (!passwordHasher.matches(command.rawPassword, user.passwordHash)) {
            loginAttempts.recordFailure(key)
            throw InvalidCredentials()
        }
        loginAttempts.clear(key)
        return user
    }
}

