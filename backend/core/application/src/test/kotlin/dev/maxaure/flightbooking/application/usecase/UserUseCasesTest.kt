package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.LoginAttemptKey
import dev.maxaure.flightbooking.application.port.out.LoginAttemptLimiter
import dev.maxaure.flightbooking.application.port.out.PasswordHasher
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.user.AuthenticateUserCommand
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.EmailAlreadyRegistered
import dev.maxaure.flightbooking.domain.model.user.InvalidCredentials
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.RegisterUserCommand
import dev.maxaure.flightbooking.domain.model.user.TooManyLoginAttempts
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserUseCasesTest {

    private val now = Instant.parse("2026-01-01T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

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

    private val users = InMemoryUsers()
    private val attempts = RecordingLoginAttempts()
    private val register = RegisterUserUseCase(users, ReversingHasher(), clock)
    private val authenticate = AuthenticateUserUseCase(users, ReversingHasher(), attempts)
    private val currentUser = GetCurrentUserUseCase(users)

    @Test
    fun `when user registers then email is stored lowercased and password is hashed`() {
        val user = register.register(RegisterUserCommand("  Anna@Example.COM ", "s3cret"))

        assertEquals(Email.of("anna@example.com"), user.email)
        assertEquals("terc3s", users.rows.getValue(user.id).passwordHash.value)
        assertEquals(now, user.createdAt)
    }

    @Test
    fun `when email already exists then registration is rejected`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))

        assertFailsWith<EmailAlreadyRegistered> {
            register.register(RegisterUserCommand("ANNA@example.com", "other"))
        }
        assertEquals(1, users.rows.size)
    }

    @Test
    fun `when credentials are correct then authentication returns the user and forgets earlier failures`() {
        val registered = register.register(RegisterUserCommand("anna@example.com", "s3cret"))
        attempts.failures[LoginAttemptKey("10.0.0.1", "anna@example.com")] = 3

        val user = authenticate.authenticate(AuthenticateUserCommand("Anna@Example.com", "s3cret", "10.0.0.1"))

        assertEquals(registered.id, user.id)
        assertEquals(emptyMap(), attempts.failures)
        assertEquals(LoginAttemptKey("10.0.0.1", "anna@example.com"), attempts.checked.single())
    }

    @Test
    fun `when password is wrong then authentication fails just like an unknown email`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))

        assertFailsWith<InvalidCredentials> {
            authenticate.authenticate(AuthenticateUserCommand("anna@example.com", "wrong", "10.0.0.1"))
        }
        assertFailsWith<InvalidCredentials> {
            authenticate.authenticate(AuthenticateUserCommand("unknown@example.com", "s3cret", "10.0.0.1"))
        }
        assertEquals(mapOf(LoginAttemptKey("10.0.0.1", "anna@example.com") to 1, LoginAttemptKey("10.0.0.1", "unknown@example.com") to 1), attempts.failures)
    }

    @Test
    fun `when the limiter blocks the address and email then the password is never hashed`() {
        register.register(RegisterUserCommand("anna@example.com", "s3cret"))
        attempts.blocked = TooManyLoginAttempts(java.time.Duration.ofMinutes(5))

        assertFailsWith<TooManyLoginAttempts> {
            authenticate.authenticate(AuthenticateUserCommand("anna@example.com", "s3cret", "10.0.0.1"))
        }
        assertEquals(emptyMap(), attempts.failures)
    }

    @Test
    fun `when current user is requested then existing user is returned and missing one is not found`() {
        val registered = register.register(RegisterUserCommand("anna@example.com", "s3cret"))

        assertEquals("anna@example.com", currentUser.currentUser(registered.id).email.value)
        assertFailsWith<NotFoundException> { currentUser.currentUser(UserId.new()) }
    }
}
