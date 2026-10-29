package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.LoginAttemptKey
import dev.maxaure.flightbooking.application.port.out.LoginAttemptLimiter
import dev.maxaure.flightbooking.application.port.out.MailSender
import dev.maxaure.flightbooking.application.port.out.PasswordHasher
import dev.maxaure.flightbooking.application.port.out.PendingRegistrationStore
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.application.port.out.VerificationCodeGenerator
import dev.maxaure.flightbooking.application.usecase.user.RegisterUserUseCase
import dev.maxaure.flightbooking.application.usecase.user.ResendVerificationUseCase
import dev.maxaure.flightbooking.application.usecase.user.VerifyEmailUseCase
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.RegisterUserCommand
import dev.maxaure.flightbooking.domain.model.user.ResendVerificationCommand
import dev.maxaure.flightbooking.domain.model.user.TooManyLoginAttempts
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand
import dev.maxaure.flightbooking.domain.model.user.verification.InvalidVerificationCode
import dev.maxaure.flightbooking.domain.model.user.verification.PendingRegistration
import dev.maxaure.flightbooking.domain.model.user.verification.ResendBackoff
import dev.maxaure.flightbooking.domain.model.user.verification.ResendOutcome
import dev.maxaure.flightbooking.domain.model.user.verification.VerificationOutcome
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VerifyEmailUseCaseTest {

    private val now = Instant.parse("2026-01-01T10:00:00Z")
    private val ttl = Duration.ofMinutes(15)
    private val address = "10.0.0.1"
    private val email = Email.of("anna@example.com")

    private class MutableClock(var current: Instant) : Clock() {
        override fun instant(): Instant = current

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this

        fun advance(duration: Duration) {
            current = current.plus(duration)
        }
    }

    private class StubHasher : PasswordHasher {
        override fun hash(rawPassword: String): PasswordHash = PasswordHash.fromTrusted("h:$rawPassword")

        override fun matches(rawPassword: String, stored: PasswordHash): Boolean = stored.value == "h:$rawPassword"
    }

    private class InMemoryLimiter : LoginAttemptLimiter {
        val failures = mutableMapOf<LoginAttemptKey, Int>()
        var blocked: TooManyLoginAttempts? = null

        override fun check(key: LoginAttemptKey) {
            blocked?.let { throw it }
        }

        override fun recordFailure(key: LoginAttemptKey) {
            failures[key] = (failures[key] ?: 0) + 1
        }

        override fun clear(key: LoginAttemptKey) {
            failures.remove(key)
        }
    }

    private class RecordingMail : MailSender {
        val sent = mutableListOf<Pair<String, Map<String, String>>>()

        override fun send(template: String, to: Email, model: Map<String, String>) {
            sent.add(to.value to model)
        }
    }

    private class SequenceCodes(private val codes: List<String>) : VerificationCodeGenerator {
        private var index = 0

        override fun next(): String = codes[index++.coerceAtMost(codes.lastIndex)]
    }

    private class InMemoryUsers : UserRepository {
        val rows = mutableMapOf<UserId, User>()

        override fun findById(id: UserId): User? = rows[id]

        override fun findByEmail(email: Email): User? = rows.values.firstOrNull { it.email == email }

        override fun save(user: User): User {
            rows[user.id] = user
            return user
        }
    }

    private class InMemoryPending(private val clock: Clock) : PendingRegistrationStore {
        val rows = mutableMapOf<String, PendingRegistration>()

        override fun find(email: Email): PendingRegistration? = rows[email.value]

        override fun save(pending: PendingRegistration): PendingRegistration {
            rows[pending.email.value] = pending
            return pending
        }

        override fun delete(email: Email): Boolean = rows.remove(email.value) != null

        override fun claimAttempt(email: Email, now: Instant): PendingRegistration? {
            val current = rows[email.value] ?: return null
            if (current.isExpiredAt(now) || current.attempts >= PendingRegistration.MAX_ATTEMPTS) return null
            val claimed = current.recordAttempt()
            rows[email.value] = claimed
            return claimed
        }

        override fun purgeExpired(before: Instant): Int {
            val expired = rows.filterValues { !it.expiresAt.isAfter(before) }.keys
            expired.forEach(rows::remove)
            return expired.size
        }
    }

    private val clock = MutableClock(now)
    private val users = InMemoryUsers()
    private val pending = InMemoryPending(clock)
    private val limiter = InMemoryLimiter()
    private val mail = RecordingMail()
    private val hasher = StubHasher()
    private val codes = SequenceCodes(listOf("123456", "654321", "999999"))

    private val register = RegisterUserUseCase(
        users, pending, hasher, clock, codes, mail, ttl, "flight-booking", ResendBackoff(ttl),
    )
    private val verify = VerifyEmailUseCase(users, pending, hasher, limiter, clock)
    private val resend = ResendVerificationUseCase(
        users, pending, hasher, limiter, codes, mail, clock, ttl, "flight-booking", ResendBackoff(ttl),
    )

    private fun pendingRow(email: Email): PendingRegistration = pending.rows.getValue(email.value)

    private fun verified(email: Email, password: String = "s3cret"): User {
        register.register(RegisterUserCommand(email.value, password))
        val outcome = verify.verify(VerifyEmailCommand(email.value, "123456", address))
        return assertIs<VerificationOutcome.Verified>(outcome).user
    }

    @Test
    fun `when the code is correct then the user is stored verified and the pending row removed`() {
        register.register(RegisterUserCommand(email.value, "s3cret"))

        val user = assertIs<VerificationOutcome.Verified>(
            verify.verify(VerifyEmailCommand("  Anna@Example.COM ", "123456", address)),
        ).user

        assertTrue(user.isVerified)
        assertEquals(now, user.emailVerifiedAt)
        assertEquals("h:s3cret", user.passwordHash.value)
        assertEquals(1, users.rows.size)
        assertNull(pending.rows[email.value])
    }

    @Test
    fun `when the same code is submitted twice then the second attempt is rejected`() {
        register.register(RegisterUserCommand(email.value, "s3cret"))
        verify.verify(VerifyEmailCommand(email.value, "123456", address))

        assertFailsWith<InvalidVerificationCode> {
            verify.verify(VerifyEmailCommand(email.value, "123456", address))
        }
        assertEquals(1, users.rows.size)
    }

    @Test
    fun `when a wrong code is submitted then the counter grows and the generic error is returned`() {
        register.register(RegisterUserCommand(email.value, "s3cret"))

        val error = assertFailsWith<InvalidVerificationCode> {
            verify.verify(VerifyEmailCommand(email.value, "000000", address))
        }

        assertEquals("The verification code is invalid or expired.", error.message)
        assertEquals(1, pendingRow(email).attempts)
        assertTrue(users.rows.isEmpty())
    }

    @Test
    fun `when the limiter refuses the request then no attempt is spent`() {
        register.register(RegisterUserCommand(email.value, "s3cret"))
        limiter.blocked = TooManyLoginAttempts(Duration.ofMinutes(1))

        assertFailsWith<TooManyLoginAttempts> {
            verify.verify(VerifyEmailCommand(email.value, "123456", address))
        }
        assertEquals(0, pendingRow(email).attempts)
    }

    @Test
    fun `when resend succeeds then the new code replaces the old one`() {
        register.register(RegisterUserCommand(email.value, "s3cret"))
        limiter.failures.clear()
        clock.advance(Duration.ofMinutes(1))

        val sent = resend.resend(ResendVerificationCommand(email.value, address))

        assertIs<ResendOutcome.Sent>(sent)
        assertEquals(2, mail.sent.size)
        assertEquals("654321", mail.sent.last().second["code"])
        assertEquals(2, pendingRow(email).sendCount)
        assertFailsWith<InvalidVerificationCode> {
            verify.verify(VerifyEmailCommand(email.value, "123456", address))
        }
        assertTrue(verify.verify(VerifyEmailCommand(email.value, "654321", address)) is VerificationOutcome.Verified)
    }

    @Test
    fun `when resend is asked again inside the cooldown then the answer carries the remaining seconds`() {
        register.register(RegisterUserCommand(email.value, "s3cret"))
        clock.advance(Duration.ofSeconds(10))

        val outcome = assertIs<ResendOutcome.CoolingDown>(
            resend.resend(ResendVerificationCommand(email.value, address)),
        )

        assertEquals(50, outcome.retryAfterSeconds)
        assertEquals(1, mail.sent.size)
    }

    @Test
    fun `when a code is sent then the outcome reports the full cooldown`() {
        val started = register.register(RegisterUserCommand(email.value, "s3cret"))

        assertEquals(60, started.retryAfterSeconds)
    }

    @Test
    fun `when resend is asked after the first cooldown then a new code goes out`() {
        register.register(RegisterUserCommand(email.value, "s3cret"))
        clock.advance(Duration.ofSeconds(61))

        val sent = resend.resend(ResendVerificationCommand(email.value, address))

        assertIs<ResendOutcome.Sent>(sent)
        assertEquals(2, mail.sent.size)
    }

    @Test
    fun `when the pending row expired then resend stays silent and register starts a fresh code`() {
        register.register(RegisterUserCommand(email.value, "s3cret"))
        clock.advance(ttl)

        assertIs<ResendOutcome.Ignored>(resend.resend(ResendVerificationCommand(email.value, address)))
        assertEquals(1, mail.sent.size)

        register.register(RegisterUserCommand(email.value, "s3cret"))

        assertEquals(2, mail.sent.size)
        assertEquals(clock.instant().plus(ttl), pendingRow(email).expiresAt)
        assertEquals(1, pendingRow(email).sendCount)
    }

    @Test
    fun `when the account already exists then resend is silent`() {
        verified(email)
        clock.advance(Duration.ofMinutes(30))
        mail.sent.clear()

        val sent = resend.resend(ResendVerificationCommand(email.value, address))

        assertIs<ResendOutcome.Ignored>(sent)
        assertTrue(mail.sent.isEmpty())
    }

    @Test
    fun `when the email is unknown then resend spends a limiter token but mails nothing`() {
        val sent = resend.resend(ResendVerificationCommand("ghost@example.com", address))

        assertIs<ResendOutcome.Ignored>(sent)
        assertTrue(mail.sent.isEmpty())
        assertEquals(1, limiter.failures[LoginAttemptKey(address, "ghost@example.com")])
    }

    @Test
    fun `when the request limiter is exhausted then resend fails even for a ghost email`() {
        limiter.blocked = TooManyLoginAttempts(Duration.ofMinutes(1))

        assertFailsWith<TooManyLoginAttempts> {
            resend.resend(ResendVerificationCommand(email.value, address))
        }
        assertTrue(mail.sent.isEmpty())
    }
}
