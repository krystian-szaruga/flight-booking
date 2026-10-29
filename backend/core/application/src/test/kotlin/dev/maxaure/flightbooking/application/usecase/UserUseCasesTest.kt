package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.LoginAttemptKey
import dev.maxaure.flightbooking.application.port.out.LoginAttemptLimiter
import dev.maxaure.flightbooking.application.port.out.MailSender
import dev.maxaure.flightbooking.application.port.out.PasswordHasher
import dev.maxaure.flightbooking.application.port.out.PendingRegistrationStore
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.application.port.out.VerificationCodeGenerator
import dev.maxaure.flightbooking.application.usecase.user.AuthenticateUserUseCase
import dev.maxaure.flightbooking.application.usecase.user.GetCurrentUserUseCase
import dev.maxaure.flightbooking.application.usecase.user.RegisterUserUseCase
import dev.maxaure.flightbooking.application.usecase.user.VerifyEmailUseCase
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.user.AuthenticateUserCommand
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.InvalidCredentials
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.RegisterUserCommand
import dev.maxaure.flightbooking.domain.model.user.TooManyLoginAttempts
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import dev.maxaure.flightbooking.domain.model.user.verification.PendingRegistration
import dev.maxaure.flightbooking.domain.model.user.verification.RegistrationStage
import dev.maxaure.flightbooking.domain.model.user.verification.ResendBackoff
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

class UserUseCasesTest {

    private val now = Instant.parse("2026-01-01T10:00:00Z")
    private val ttl = Duration.ofMinutes(15)
    private val address = "10.0.0.1"

    private class MutableClock(var current: Instant) : Clock() {
        override fun instant(): Instant = current

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        fun advance(duration: Duration) {
            current = current.plus(duration)
        }
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

    private class ReversingHasher : PasswordHasher {
        override fun hash(rawPassword: String): PasswordHash = PasswordHash.fromTrusted(rawPassword.reversed())

        override fun matches(rawPassword: String, stored: PasswordHash): Boolean =
            stored.value == rawPassword.reversed()
    }

    private class RecordingLoginAttempts : LoginAttemptLimiter {
        val failures = mutableMapOf<LoginAttemptKey, Int>()
        val checked = mutableListOf<LoginAttemptKey>()
        var blocked: TooManyLoginAttempts? = null

        override fun check(key: LoginAttemptKey) {
            checked.add(key)
            blocked?.let { throw it }
        }

        override fun recordFailure(key: LoginAttemptKey) {
            failures[key] = (failures[key] ?: 0) + 1
        }

        override fun clear(key: LoginAttemptKey) {
            failures.remove(key)
        }
    }

    private class InMemoryPending() : PendingRegistrationStore {
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

    private class RecordingMailSender : MailSender {
        data class Sent(val template: String, val to: Email, val model: Map<String, String>)

        val sent = mutableListOf<Sent>()

        override fun send(template: String, to: Email, model: Map<String, String>) {
            sent.add(Sent(template, to, model))
        }
    }

    private val fixedCode = object : VerificationCodeGenerator {
        override fun next(): String = "123456"
    }

    private val clock = MutableClock(now)
    private val users = InMemoryUsers()
    private val attempts = RecordingLoginAttempts()
    private val pending = InMemoryPending()
    private val mails = RecordingMailSender()
    private val register = RegisterUserUseCase(
        users, pending, ReversingHasher(), clock, fixedCode, mails, ttl, "flight-booking", ResendBackoff(ttl),
    )
    private val verify = VerifyEmailUseCase(users, pending, ReversingHasher(), attempts, clock)
    private val authenticate = AuthenticateUserUseCase(users, ReversingHasher(), attempts)
    private val currentUser = GetCurrentUserUseCase(users)

    @Test
    fun `when a new email registers then only a pending row is stored and the code is mailed`() {
        val started = register.register(RegisterUserCommand("  Anna@Example.COM ", "s3cret"))

        assertEquals(RegistrationStage.CODE_SENT, started.stage)
        assertEquals(emptyMap(), users.rows)
        val row = pending.rows.getValue("anna@example.com")
        assertEquals("terc3s", row.passwordHash.value)
        assertEquals(now.plus(ttl), row.expiresAt)
        assertEquals(1, row.sendCount)
        assertEquals("123456", mails.sent.single().model["code"])
    }

    @Test
    fun `when registration is repeated then the pending row is replaced and a fresh code is mailed`() {
        register.register(RegisterUserCommand("anna@example.com", "first-pass"))
        clock.advance(Duration.ofMinutes(5))

        val second = register.register(RegisterUserCommand("anna@example.com", "second-pass"))

        assertEquals(RegistrationStage.CODE_SENT, second.stage)
        assertEquals(1, pending.rows.size)
        assertEquals("ssap-dnoces", pending.rows.getValue("anna@example.com").passwordHash.value)
        assertEquals(2, mails.sent.size)
    }

    @Test
    fun `when the email already has an account then register answers the same but mails nothing`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))
        val outcome = verify.verify(
            dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand("anna@example.com", "123456", address),
        )
        assertIs<VerificationOutcome.Verified>(outcome)
        mails.sent.clear()

        val started = register.register(RegisterUserCommand("ANNA@example.com", "other-pass"))

        assertEquals(RegistrationStage.EMAIL_VERIFIED, started.stage)
        assertEquals(1, users.rows.size)
        assertEquals(emptyMap(), pending.rows)
        assertEquals(emptyList(), mails.sent)
    }

    @Test
    fun `when the code is correct then the account is created verified and the pending row is gone`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))
        attempts.failures[LoginAttemptKey(address, "anna@example.com")] = 2

        val outcome = verify.verify(
            dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand("Anna@Example.com", "123456", address),
        )

        val verified = assertIs<VerificationOutcome.Verified>(outcome).user
        assertEquals("anna@example.com", verified.email.value)
        assertEquals("terc3s", verified.passwordHash.value)
        assertEquals(now, verified.emailVerifiedAt)
        assertEquals(now, verified.createdAt)
        assertNull(pending.rows["anna@example.com"])
        assertEquals(emptyMap(), attempts.failures)
    }

    @Test
    fun `when the code is wrong then the attempt is spent and no account appears`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))

        assertFailsWith<dev.maxaure.flightbooking.domain.model.user.verification.InvalidVerificationCode> {
            verify.verify(
                dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand("anna@example.com", "000000", address),
            )
        }

        assertEquals(1, pending.rows.getValue("anna@example.com").attempts)
        assertEquals(emptyMap(), users.rows)
        assertEquals(1, attempts.failures[LoginAttemptKey(address, "anna@example.com")])
    }

    @Test
    fun `when five codes are wrong then even the right code is rejected and no account appears`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))

        repeat(PendingRegistration.MAX_ATTEMPTS) {
            assertFailsWith<dev.maxaure.flightbooking.domain.model.user.verification.InvalidVerificationCode> {
                verify.verify(
                    dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand("anna@example.com", "000000", address),
                )
            }
        }
        assertFailsWith<dev.maxaure.flightbooking.domain.model.user.verification.InvalidVerificationCode> {
            verify.verify(
                dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand("anna@example.com", "123456", address),
            )
        }

        assertEquals(emptyMap(), users.rows)
        assertEquals(PendingRegistration.MAX_ATTEMPTS, pending.rows.getValue("anna@example.com").attempts)
    }

    @Test
    fun `when the pending row expired then the right code no longer works`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))
        clock.advance(ttl)

        assertFailsWith<dev.maxaure.flightbooking.domain.model.user.verification.InvalidVerificationCode> {
            verify.verify(
                dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand("anna@example.com", "123456", address),
            )
        }

        assertEquals(emptyMap(), users.rows)
    }

    @Test
    fun `when the email was never registered then verification fails like a wrong code`() {
        val ghost = assertFailsWith<dev.maxaure.flightbooking.domain.model.user.verification.InvalidVerificationCode> {
            verify.verify(
                dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand("ghost@example.com", "123456", address),
            )
        }
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))
        val wrong = assertFailsWith<dev.maxaure.flightbooking.domain.model.user.verification.InvalidVerificationCode> {
            verify.verify(
                dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand("anna@example.com", "000000", address),
            )
        }

        assertEquals(wrong.message, ghost.message)
    }

    @Test
    fun `when credentials are correct then authentication returns the verified user and forgets failures`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))
        val verified = (
            verify.verify(
                dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand("anna@example.com", "123456", address),
            ) as VerificationOutcome.Verified
            ).user
        attempts.failures[LoginAttemptKey(address, "anna@example.com")] = 3

        val user = authenticate.authenticate(AuthenticateUserCommand("Anna@Example.com", "s3cret", address))

        assertEquals(verified.id, user.id)
        assertEquals(emptyMap(), attempts.failures)
    }

    @Test
    fun `when the address only has a pending registration then login is rejected as unknown`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))

        assertFailsWith<InvalidCredentials> {
            authenticate.authenticate(AuthenticateUserCommand("anna@example.com", "s3cret", address))
        }
    }

    @Test
    fun `when the password is wrong then authentication fails just like an unknown email`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))
        verify.verify(
            dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand("anna@example.com", "123456", address),
        )

        assertFailsWith<InvalidCredentials> {
            authenticate.authenticate(AuthenticateUserCommand("anna@example.com", "wrong", address))
        }
        assertFailsWith<InvalidCredentials> {
            authenticate.authenticate(AuthenticateUserCommand("unknown@example.com", "s3cret", address))
        }
        assertEquals(
            mapOf(
                LoginAttemptKey(address, "anna@example.com") to 1,
                LoginAttemptKey(address, "unknown@example.com") to 1,
            ),
            attempts.failures,
        )
    }

    @Test
    fun `when the limiter blocks the address and email then the password is never hashed`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))
        verify.verify(
            dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand("anna@example.com", "123456", address),
        )
        attempts.blocked = TooManyLoginAttempts(Duration.ofMinutes(5))

        assertFailsWith<TooManyLoginAttempts> {
            authenticate.authenticate(AuthenticateUserCommand("anna@example.com", "s3cret", address))
        }
        assertEquals(emptyMap(), attempts.failures)
    }

    @Test
    fun `when the current user is requested then an existing user is returned and a missing one is not found`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))
        val verified = (
            verify.verify(
                dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand("anna@example.com", "123456", address),
            ) as VerificationOutcome.Verified
            ).user

        assertEquals("anna@example.com", currentUser.currentUser(verified.id).email.value)
        assertFailsWith<NotFoundException> { currentUser.currentUser(UserId.new()) }
    }

    @Test
    fun `when early rows have expired then the purge removes only those still past their ttl`() {
        register.register(RegisterUserCommand("old@example.com", "s3cret"))
        clock.advance(Duration.ofMinutes(15))
        register.register(RegisterUserCommand("fresh@example.com", "s3cret"))

        val purged = pending.purgeExpired(clock.instant())

        assertEquals(1, purged)
        assertEquals(setOf("fresh@example.com"), pending.rows.keys)
    }
}
