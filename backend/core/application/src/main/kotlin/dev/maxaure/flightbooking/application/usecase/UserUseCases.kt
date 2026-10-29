package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.AuthenticateUser
import dev.maxaure.flightbooking.application.port.`in`.GetCurrentUser
import dev.maxaure.flightbooking.application.port.`in`.RegisterUser
import dev.maxaure.flightbooking.application.port.out.LoginAttemptKey
import dev.maxaure.flightbooking.application.port.out.LoginAttemptLimiter
import dev.maxaure.flightbooking.application.port.out.PasswordHasher
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.user.AuthenticateUserCommand
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.EmailAlreadyRegistered
import dev.maxaure.flightbooking.domain.model.user.InvalidCredentials
import dev.maxaure.flightbooking.domain.model.user.RegisterUserCommand
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Clock

class RegisterUserUseCase(
    private val users: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val clock: Clock,
) : RegisterUser {

    override fun register(command: RegisterUserCommand): User {
        val email = Email.of(command.email)
        if (users.findByEmail(email) != null) throw EmailAlreadyRegistered(email)
        val user = User.register(email, passwordHasher.hash(command.rawPassword), clock.instant())
        return users.save(user)
    }
}

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

class GetCurrentUserUseCase(
    private val users: UserRepository,
) : GetCurrentUser {

    override fun currentUser(userId: UserId): User =
        users.findById(userId) ?: throw NotFoundException("User $userId does not exist")
}
