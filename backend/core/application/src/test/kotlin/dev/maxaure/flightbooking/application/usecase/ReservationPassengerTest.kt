package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.HoldSeatRequest
import dev.maxaure.flightbooking.application.port.out.HoldOutcome
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.domain.ConflictException
import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.reservation.PassengerType
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.reservation.ReservationStatus
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.ProfileIncomplete
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReservationPassengerTest {

    private val now = Instant.parse("2026-10-27T10:00:00Z")
    private val clock = object : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }

    private val flightId = UUID.randomUUID()
    private val repository = InMemoryReservationRepository()
    private val userId = UserId.new()
    private val users = InMemoryUserRepository(userWithName())
    private val useCase = ReservationUseCase(repository, users, clock)

    private fun userWithName(
        id: UserId = userId,
        firstName: String? = "Ada",
        lastName: String? = "Lovelace",
    ) = User.verified(Email.of("${id.value}@example.com"), PasswordHash.fromTrusted("hash"), now, id)
        .copy(firstName = firstName, lastName = lastName)

    private fun self(seat: String) =
        HoldSeatRequest(SeatNumber.of(seat), PassengerType.SELF, passportNumber = "X1234567")

    private fun other(seat: String, firstName: String?, lastName: String?) =
        HoldSeatRequest(SeatNumber.of(seat), PassengerType.OTHER, firstName, lastName, passportNumber = "X1234567")

    @Test
    fun `when a seat is booked for the account holder then the passenger name comes from the profile`() {
        val hold = useCase.hold(userId, flightId, listOf(self("12A")))

        assertEquals("Ada", hold.seats.single().firstName)
        assertEquals("Lovelace", hold.seats.single().lastName)
    }

    @Test
    fun `when a seat is booked for the account holder without a profile name then the hold is rejected`() {
        users.save(userWithName(firstName = null, lastName = null))

        assertFailsWith<ProfileIncomplete> { useCase.hold(userId, flightId, listOf(self("12A"))) }

        assertEquals(emptyList(), repository.heldSeats(flightId))
    }

    @Test
    fun `when a seat is booked for someone else then the provided name is used and the profile is ignored`() {
        val hold = useCase.hold(userId, flightId, listOf(other("12B", "Grace", "Hopper")))

        assertEquals("Grace", hold.seats.single().firstName)
        assertEquals("Hopper", hold.seats.single().lastName)
    }

    @Test
    fun `when a seat for someone else has no last name then the hold is rejected`() {
        assertFailsWith<InvalidDomainValueException> {
            useCase.hold(userId, flightId, listOf(other("12B", "Grace", null)))
        }

        assertEquals(emptyList(), repository.heldSeats(flightId))
    }

    @Test
    fun `when a booking mixes the account holder and a guest then each seat keeps its own name`() {
        val hold = useCase.hold(userId, flightId, listOf(self("12A"), other("12B", "Grace", "Hopper")))

        val bySeat = hold.seats.associateBy { it.seatNumber.value }
        assertTrue(bySeat.getValue("12A").fullName == "Ada Lovelace")
        assertTrue(bySeat.getValue("12B").fullName == "Grace Hopper")
    }

    private class InMemoryUserRepository(vararg initial: User) : UserRepository {
        private val users = initial.associateBy { it.id }.toMutableMap()

        override fun findById(id: UserId): User? = users[id]

        override fun findByEmail(email: Email): User? = users.values.firstOrNull { it.email == email }

        override fun save(user: User): User = user.also { users[it.id] = it }
    }

    private class InMemoryReservationRepository : ReservationRepository {
        private val reservations = linkedMapOf<UUID, Reservation>()
        private val held = mutableMapOf<UUID, MutableSet<String>>()

        fun heldSeats(flightId: UUID): List<String> =
            reservations.values
                .filter { it.flightId == flightId }
                .flatMap { held[it.id].orEmpty() }
                .sorted()

        override fun findById(id: UUID): Reservation? = reservations[id]

        override fun save(reservation: Reservation): Reservation = error("Use holdSeats or releaseSeats")

        override fun holdSeats(reservation: Reservation): HoldOutcome {
            val taken = held.values.flatten().toSet()
            val wanted = reservation.seats.map { it.seatNumber.value }
            val clash = wanted.filter { it in taken }
            if (clash.isNotEmpty()) return HoldOutcome.SeatUnavailable(clash)
            reservations[reservation.id] = reservation
            held[reservation.id] = wanted.toMutableSet()
            return HoldOutcome.Held(reservation)
        }

        override fun releaseSeats(reservationId: UUID): Reservation? {
            val current = reservations[reservationId] ?: return null
            if (current.status != ReservationStatus.PENDING) {
                throw ConflictException("Reservation $reservationId is ${current.status.name.lowercase()}")
            }
            held.remove(reservationId)
            val released = current.cancel()
            reservations[reservationId] = released
            return released
        }

        override fun releaseSeatSubset(reservationId: UUID, seatNumbers: List<String>): Reservation? =
            error("Not used by these tests")

        override fun confirm(reservationId: UUID): Reservation? = error("Not used by these tests")

        override fun findExpired(at: Instant, limit: Int): List<Reservation> = emptyList()

        override fun findByUserId(userId: UUID): List<Reservation> =
            reservations.values.filter { it.userId == userId }
    }
}
