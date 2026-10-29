package dev.maxaure.flightbooking.application.usecase.user

import dev.maxaure.flightbooking.application.port.`in`.VerifyEmail
import dev.maxaure.flightbooking.application.port.out.LoginAttemptKey
import dev.maxaure.flightbooking.application.port.out.LoginAttemptLimiter
import dev.maxaure.flightbooking.application.port.out.PasswordHasher
import dev.maxaure.flightbooking.application.port.out.PendingRegistrationStore
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.EmailAlreadyRegistered
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand
import dev.maxaure.flightbooking.domain.model.user.verification.InvalidVerificationCode
import dev.maxaure.flightbooking.domain.model.user.verification.VerificationOutcome
import java.time.Clock

class VerifyEmailUseCase(
    private val users: UserRepository,
    private val pending: PendingRegistrationStore,
    private val passwordHasher: PasswordHasher,
    private val loginAttempts: LoginAttemptLimiter,
    private val clock: Clock,
) : VerifyEmail {

    override fun verify(command: VerifyEmailCommand): VerificationOutcome {
        val email = runCatching { Email.of(command.email) }.getOrElse { throw InvalidVerificationCode() }
        val key = LoginAttemptKey(command.address, email.value)
        loginAttempts.check(key)

        val now = clock.instant()
        val claimed = pending.claimAttempt(email, now) ?: throw reject(key)
        if (!passwordHasher.matches(command.code, PasswordHash.fromTrusted(claimed.codeHash))) throw reject(key)

        if (users.findByEmail(email) != null) {
            pending.delete(email)
            return VerificationOutcome.AlreadyRegistered
        }

        return try {
            val user = users.save(claimed.toUser(now))
            pending.delete(email)
            loginAttempts.clear(key)
            VerificationOutcome.Verified(user)
        } catch (e: EmailAlreadyRegistered) {
            pending.delete(email)
            VerificationOutcome.AlreadyRegistered
        }
    }

    private fun reject(key: LoginAttemptKey): InvalidVerificationCode {
        loginAttempts.recordFailure(key)
        return InvalidVerificationCode()
    }
}
