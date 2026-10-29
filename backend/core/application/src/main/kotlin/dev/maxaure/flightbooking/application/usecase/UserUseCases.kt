package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.AuthenticateUser
import dev.maxaure.flightbooking.application.port.`in`.GetCurrentUser
import dev.maxaure.flightbooking.application.port.`in`.RegisterUser
import dev.maxaure.flightbooking.application.port.`in`.ResendVerification
import dev.maxaure.flightbooking.application.port.`in`.VerifyEmail
import dev.maxaure.flightbooking.application.port.out.LoginAttemptKey
import dev.maxaure.flightbooking.application.port.out.LoginAttemptLimiter
import dev.maxaure.flightbooking.application.port.out.MailSender
import dev.maxaure.flightbooking.application.port.out.PasswordHasher
import dev.maxaure.flightbooking.application.port.out.PendingRegistrationStore
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.application.port.out.VerificationCodeGenerator
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.user.AuthenticateUserCommand
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.EmailAlreadyRegistered
import dev.maxaure.flightbooking.domain.model.user.InvalidCredentials
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.RegisterUserCommand
import dev.maxaure.flightbooking.domain.model.user.ResendVerificationCommand
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand
import dev.maxaure.flightbooking.domain.model.user.verification.InvalidVerificationCode
import dev.maxaure.flightbooking.domain.model.user.verification.PendingRegistration
import dev.maxaure.flightbooking.domain.model.user.verification.RegistrationStage
import dev.maxaure.flightbooking.domain.model.user.verification.RegistrationStarted
import dev.maxaure.flightbooking.domain.model.user.verification.ResendBackoff
import dev.maxaure.flightbooking.domain.model.user.verification.ResendOutcome
import dev.maxaure.flightbooking.domain.model.user.verification.VerificationOutcome
import java.time.Clock
import java.time.Duration
import java.time.Instant

class RegisterUserUseCase(
    private val users: UserRepository,
    private val pending: PendingRegistrationStore,
    private val passwordHasher: PasswordHasher,
    private val clock: Clock,
    private val codeGenerator: VerificationCodeGenerator,
    private val mail: MailSender,
    private val codeTtl: Duration,
    private val appName: String,
    private val backoff: ResendBackoff,
) : RegisterUser {

    override fun register(command: RegisterUserCommand): RegistrationStarted {
        val email = Email.of(command.email)
        val now = clock.instant()
        val code = codeGenerator.next()

        if (users.findByEmail(email) != null) {
            passwordHasher.hash(command.rawPassword)
            passwordHasher.hash(code)
            return RegistrationStarted(RegistrationStage.EMAIL_VERIFIED, 0)
        }

        val passwordHash = passwordHasher.hash(command.rawPassword)
        val codeHash = passwordHasher.hash(code)
        val stored = PendingRegistration.start(email, passwordHash, codeHash.value, now.plus(codeTtl), now)
        pending.save(stored)
        mail.send(TEMPLATE, email, model(code))
        return RegistrationStarted(RegistrationStage.CODE_SENT, cooldownSeconds(stored, now))
    }

    private fun cooldownSeconds(pending: PendingRegistration, now: Instant): Long =
        backoff.remainingCooldown(PendingRegistration.resendStateOf(pending), now).seconds.coerceAtLeast(0)

    private fun model(code: String): Map<String, String> =
        mapOf("appName" to appName, "code" to code, "ttlMinutes" to codeTtl.toMinutes().toString())

    private companion object {
        const val TEMPLATE = "verification"
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

class ResendVerificationUseCase(
    private val users: UserRepository,
    private val pending: PendingRegistrationStore,
    private val passwordHasher: PasswordHasher,
    private val loginAttempts: LoginAttemptLimiter,
    private val codeGenerator: VerificationCodeGenerator,
    private val mail: MailSender,
    private val clock: Clock,
    private val codeTtl: Duration,
    private val appName: String,
    private val backoff: ResendBackoff,
) : ResendVerification {

    override fun resend(command: ResendVerificationCommand): ResendOutcome {
        val key = LoginAttemptKey(command.address, normalise(command.email))
        loginAttempts.check(key)
        loginAttempts.recordFailure(key)

        val email = runCatching { Email.of(command.email) }.getOrElse { return ResendOutcome.Ignored }
        if (users.findByEmail(email) != null) return ResendOutcome.Ignored
        val current = pending.find(email) ?: return ResendOutcome.Ignored

        val now = clock.instant()
        if (current.isExpiredAt(now)) return ResendOutcome.Ignored

        val coolingDown = backoff.remainingCooldown(PendingRegistration.resendStateOf(current), now)
        if (coolingDown.isPositive) {
            return ResendOutcome.CoolingDown(coolingDown.seconds.coerceAtLeast(1))
        }

        val code = codeGenerator.next()
        val renewed = current.replaceCode(passwordHasher.hash(code).value, now.plus(codeTtl), now)
        pending.save(renewed)
        mail.send(TEMPLATE, email, model(code))
        return ResendOutcome.Sent(backoff.remainingCooldown(PendingRegistration.resendStateOf(renewed), now).seconds)
    }

    private fun normalise(raw: String): String = raw.trim().lowercase()

    private fun model(code: String): Map<String, String> =
        mapOf("appName" to appName, "code" to code, "ttlMinutes" to codeTtl.toMinutes().toString())

    private companion object {
        const val TEMPLATE = "verification"
    }
}
