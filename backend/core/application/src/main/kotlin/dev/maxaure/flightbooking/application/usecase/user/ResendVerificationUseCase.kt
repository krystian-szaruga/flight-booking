package dev.maxaure.flightbooking.application.usecase.user

import dev.maxaure.flightbooking.application.port.`in`.ResendVerification
import dev.maxaure.flightbooking.application.port.out.LoginAttemptKey
import dev.maxaure.flightbooking.application.port.out.LoginAttemptLimiter
import dev.maxaure.flightbooking.application.port.out.MailSender
import dev.maxaure.flightbooking.application.port.out.PasswordHasher
import dev.maxaure.flightbooking.application.port.out.PendingRegistrationStore
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.application.port.out.VerificationCodeGenerator
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.ResendVerificationCommand
import dev.maxaure.flightbooking.domain.model.user.verification.PendingRegistration
import dev.maxaure.flightbooking.domain.model.user.verification.ResendBackoff
import dev.maxaure.flightbooking.domain.model.user.verification.ResendOutcome
import java.time.Clock
import java.time.Duration

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
