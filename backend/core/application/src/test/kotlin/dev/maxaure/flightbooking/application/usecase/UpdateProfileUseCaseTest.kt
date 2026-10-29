package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.application.usecase.user.UpdateProfileUseCase
import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.UpdateProfileCommand
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UpdateProfileUseCaseTest {

    private val now = Instant.parse("2026-10-27T10:00:00Z")
    private val userId = UserId.new()
    private val users = InMemoryUserRepository(
        User.verified(Email.of("ada@example.com"), PasswordHash.fromTrusted("hash"), now, userId),
    )
    private val useCase = UpdateProfileUseCase(users)

    @Test
    fun `when a visitor sets their name then it is stored trimmed and returned`() {
        val updated = useCase.update(userId, UpdateProfileCommand("  Ada ", " Lovelace "))

        assertEquals("Ada", updated.firstName)
        assertEquals("Lovelace", updated.lastName)
        assertEquals("Ada", users.findById(userId)?.firstName)
    }

    @Test
    fun `when only a first name is given then the update is rejected and the profile stays empty`() {
        assertFailsWith<InvalidDomainValueException> { useCase.update(userId, UpdateProfileCommand("Ada", "   ")) }

        assertNull(users.findById(userId)?.firstName)
        assertNull(users.findById(userId)?.lastName)
    }

    @Test
    fun `when an unknown user updates their profile then it is reported as not found`() {
        assertFailsWith<NotFoundException> { useCase.update(UserId.new(), UpdateProfileCommand("Ada", "Lovelace")) }
    }

    private class InMemoryUserRepository(vararg initial: User) : UserRepository {
        private val users = initial.associateBy { it.id }.toMutableMap()

        override fun findById(id: UserId): User? = users[id]

        override fun findByEmail(email: Email): User? = users.values.firstOrNull { it.email == email }

        override fun save(user: User): User = user.also { users[it.id] = it }
    }
}
