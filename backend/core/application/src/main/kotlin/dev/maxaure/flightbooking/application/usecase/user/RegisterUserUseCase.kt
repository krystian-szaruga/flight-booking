package dev.maxaure.flightbooking.application.usecase.user

import dev.maxaure.flightbooking.application.port.`in`.RegisterUser
import dev.maxaure.flightbooking.application.port.out.MailSender
import dev.maxaure.flightbooking.application.port.out.PasswordHasher
import dev.maxaure.flightbooking.application.port.out.PendingRegistrationStore
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.application.port.out.VerificationCodeGenerator
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.RegisterUserCommand
import dev.maxaure.flightbooking.domain.model.user.verification.PendingRegistration
import dev.maxaure.flightbooking.domain.model.user.verification.RegistrationStage
import dev.maxaure.flightbooking.domain.model.user.verification.RegistrationStarted
import dev.maxaure.flightbooking.domain.model.user.verification.ResendBackoff
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





