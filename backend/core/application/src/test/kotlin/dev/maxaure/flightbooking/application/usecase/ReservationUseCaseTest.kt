package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.HoldSeatRequest
import dev.maxaure.flightbooking.application.port.out.FlightRepository
import dev.maxaure.flightbooking.application.port.out.HoldOutcome
import dev.maxaure.flightbooking.application.port.out.NotificationPublisher
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.domain.ConflictException
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.catalogue.Airport
import dev.maxaure.flightbooking.domain.model.catalogue.Flight
import dev.maxaure.flightbooking.domain.model.catalogue.FlightIdentity
import dev.maxaure.flightbooking.domain.model.catalogue.FlightStatus
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.notification.NotificationEvent
import dev.maxaure.flightbooking.domain.model.pricing.Cabin
import dev.maxaure.flightbooking.domain.model.pricing.FareClass
import dev.maxaure.flightbooking.domain.model.pricing.Price
import dev.maxaure.flightbooking.domain.model.reservation.PassengerSeat
import dev.maxaure.flightbooking.domain.model.reservation.PassengerType
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.reservation.ReservationStatus
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.seat.Seat
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReservationUseCaseTest {

    private var now = Instant.parse("2026-10-27T10:00:00Z")
    private val clock = object : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }

    private val flightId = UUID.randomUUID()
    private val repository = InMemoryReservationRepository { now }
    private val userId = UserId.new()
    private val users = InMemoryUserRepository(createUser(userId))
    private val flight = Flight(
        id = flightId,
        flightNumber = FlightNumber.of("LO381"),
        origin = Airport.create("WAW", "Warsaw Chopin", "Warsaw", "PL"),
        destination = Airport.create("LIS", "Lisbon", "Lisbon", "PT"),
        departureAt = now.plus(Duration.ofDays(3)),
        arrivalAt = now.plus(Duration.ofDays(3)).plus(Duration.ofHours(4)),
        aircraft = "A320neo",
        status = FlightStatus.SCHEDULED,
        fares = listOf(FareClass(UUID.randomUUID(), Cabin.ECONOMY, Price(BigDecimal("250.00"), "PLN"))),
    )
    private val flights = object : FlightRepository {
        override fun findById(id: UUID): Flight? = flight.takeIf { it.id == id }
        override fun findByIdentity(identity: FlightIdentity): Flight? = flight.takeIf { it.identity == identity }
        override fun save(flight: Flight): Flight = flight
        override fun replaceSeatInventory(flightId: UUID, seats: List<Seat>) = Unit
        override fun countSeats(flightId: UUID): Int = 0
        override fun countSoldSeats(flightId: UUID): Int = 0
        override fun findDeparturesBetween(from: Instant, before: Instant): List<Flight> = emptyList()
    }
    private val published = mutableListOf<NotificationEvent>()
    private val notifications = object : NotificationPublisher {
        override fun publish(event: NotificationEvent) {
            published += event
        }
    }
    private val useCase = ReservationUseCase(repository, users, flights, notifications, clock)

    private fun createUser(id: UserId) =
        User.verified(Email.of("${id.value}@example.com"), PasswordHash.fromTrusted("hash"), now, id)

    private fun seats(vararg numbers: String) =
        numbers.map {
            HoldSeatRequest(
                SeatNumber.of(it),
                PassengerType.OTHER,
                "Passenger",
                it,
                passportNumber = "X1234567",
                phoneNumber = "+48600123456",
                dateOfBirth = LocalDate.of(1990, 1, 1),
            )
        }

    @Test
    fun `when a seat is already held then a second hold of the same seat is rejected and the first survives`() {
        val first = useCase.hold(userId, flightId, seats("12A", "12B"))

        val failure = assertFailsWith<ConflictException> { useCase.hold(userId, flightId, seats("12B", "12C")) }

        assertTrue("12B" in failure.message.orEmpty())
        assertEquals(listOf("12A", "12B"), repository.heldSeats(flightId))
        assertEquals(ReservationStatus.PENDING, repository.findById(first.id)?.status)
    }

    @Test
    fun `when a conflicting hold is rejected then none of its other seats stay held`() {
        useCase.hold(userId, flightId, seats("12A"))

        assertFailsWith<ConflictException> { useCase.hold(userId, flightId, seats("12C", "12A")) }

        assertEquals(listOf("12A"), repository.heldSeats(flightId))
    }

    @Test
    fun `when one seat of a hold is released then only that seat becomes available again`() {
        val hold = useCase.hold(userId, flightId, seats("12A", "12B", "12C"))

        val updated = useCase.cancelSeats(userId, hold.id, listOf(SeatNumber.of("12B")))

        assertEquals(listOf("12A", "12C"), updated.seats.map { it.seatNumber.value })
        assertEquals(listOf("12A", "12C"), repository.heldSeats(flightId))
        assertEquals(ReservationStatus.PENDING, repository.findById(hold.id)?.status)
    }

    @Test
    fun `when every seat of a hold is released then the whole reservation is cancelled`() {
        val hold = useCase.hold(userId, flightId, seats("12A", "12B"))

        val released = useCase.cancelSeats(userId, hold.id, listOf(SeatNumber.of("12A"), SeatNumber.of("12B")))

        assertEquals(ReservationStatus.CANCELLED, released.status)
        assertEquals(emptyList(), repository.heldSeats(flightId))
    }

    @Test
    fun `when a seat that is not part of the hold is released then nothing changes`() {
        val hold = useCase.hold(userId, flightId, seats("12A"))

        assertFailsWith<NotFoundException> { useCase.cancelSeats(userId, hold.id, listOf(SeatNumber.of("13C"))) }

        assertEquals(listOf("12A"), repository.heldSeats(flightId))
        assertEquals(ReservationStatus.PENDING, repository.findById(hold.id)?.status)
    }

    @Test
    fun `when a hold is cancelled then its seats become available again`() {
        val hold = useCase.hold(userId, flightId, seats("12A", "12B"))

        val cancelled = useCase.cancel(userId, hold.id)

        assertEquals(ReservationStatus.CANCELLED, cancelled.status)
        assertNull(cancelled.expiresAt)
        assertEquals(emptyList(), repository.heldSeats(flightId))
    }

    @Test
    fun `when a hold has expired then the sweeper frees its seats and marks it expired`() {
        val stale = useCase.hold(userId, flightId, seats("12A"))
        now = now.plus(Duration.ofMinutes(10))
        val fresh = useCase.hold(userId, flightId, seats("12B"))
        now = now.plus(Duration.ofMinutes(6))

        val released = useCase.releaseExpired()

        assertEquals(listOf(stale.id), released)
        assertEquals(listOf("12B"), repository.heldSeats(flightId))
        assertEquals(ReservationStatus.EXPIRED, repository.findById(stale.id)?.status)
        assertEquals(ReservationStatus.PENDING, repository.findById(fresh.id)?.status)
    }

    @Test
    fun `when the sweeper expires a hold then the owner is told which seats were released`() {
        val stale = useCase.hold(userId, flightId, seats("12A", "12B"))
        now = now.plus(Duration.ofMinutes(16))

        useCase.releaseExpired()

        val event = published.single() as NotificationEvent.ReservationExpired
        assertEquals(createUser(userId).email, event.recipient)
        assertEquals(listOf("12A", "12B"), event.seats.map { it.value })
    }

    @Test
    fun `when an expired hold is cancelled by the client then it is reported as a conflict`() {
        val stale = useCase.hold(userId, flightId, seats("12A"))
        now = now.plus(Duration.ofMinutes(16))
        useCase.releaseExpired()

        assertFailsWith<ConflictException> { useCase.cancel(userId, stale.id) }
    }

    @Test
    fun `when an unknown reservation is cancelled then it is reported as not found`() {
        assertFailsWith<NotFoundException> { useCase.cancel(userId, UUID.randomUUID()) }
    }

    @Test
    fun `when a hold is placed then it is recorded against the authenticated user`() {
        val hold = useCase.hold(userId, flightId, seats("12A"))

        assertEquals(userId.value, repository.findById(hold.id)?.userId)
    }

    @Test
    fun `when an unknown user places a hold then it is rejected before any seat is taken`() {
        val stranger = UserId.new()

        assertFailsWith<NotFoundException> { useCase.hold(stranger, flightId, seats("12A")) }

        assertEquals(emptyList(), repository.heldSeats(flightId))
    }

    @Test
    fun `when user B cancels user A's reservation then it is reported as not found and the hold survives`() {
        val hold = useCase.hold(userId, flightId, seats("12A"))
        val intruder = UserId.new()
        users.save(createUser(intruder))

        assertFailsWith<NotFoundException> { useCase.cancel(intruder, hold.id) }

        assertEquals(listOf("12A"), repository.heldSeats(flightId))
        assertEquals(ReservationStatus.PENDING, repository.findById(hold.id)?.status)
    }

    @Test
    fun `when the committed reservation is cancelled by its owner then nobody else's reservation is touched`() {
        val mine = useCase.hold(userId, flightId, seats("12A"))
        val other = UserId.new()
        users.save(createUser(other))
        val theirs = useCase.hold(other, flightId, seats("12B"))

        val cancelled = useCase.cancel(userId, mine.id)

        assertEquals(ReservationStatus.CANCELLED, cancelled.status)
        assertEquals(userId.value, cancelled.userId)
        assertEquals(listOf("12B"), repository.heldSeats(flightId))
        assertEquals(ReservationStatus.PENDING, repository.findById(theirs.id)?.status)
    }

    private class InMemoryUserRepository(vararg initial: User) : UserRepository {
        private val users = initial.associateBy { it.id }.toMutableMap()

        override fun findById(id: UserId): User? = users[id]

        override fun findByEmail(email: Email): User? = users.values.firstOrNull { it.email == email }

        override fun save(user: User): User = user.also { users[it.id] = it }
    }

    private class InMemoryReservationRepository(
        private val now: () -> Instant,
    ) : ReservationRepository {
        private val reservations = linkedMapOf<UUID, Reservation>()
        private val held = linkedMapOf<UUID, MutableSet<String>>()

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
            val released = when (current.status) {
                ReservationStatus.PENDING ->
                    if (!current.expiresAt!!.isAfter(now())) current.expire(now()) else current.cancel()
                else -> throw ConflictException("Reservation $reservationId is ${current.status.name.lowercase()}")
            }
            held.remove(reservationId)
            reservations[reservationId] = released
            return released
        }

        override fun releaseSeatSubset(reservationId: UUID, seatNumbers: List<String>): Reservation? {
            val current = reservations[reservationId] ?: return null
            if (current.status != ReservationStatus.PENDING) return current
            val remaining = current.seats.filter { it.seatNumber.value !in seatNumbers }
            held[reservationId]?.removeAll(seatNumbers.toSet())
            val updated = current.copy(seats = remaining)
            reservations[reservationId] = updated
            return updated
        }

        override fun updatePassengers(reservationId: UUID, seats: List<PassengerSeat>): Reservation? {
            val current = reservations[reservationId] ?: return null
            return current.copy(seats = seats).also { reservations[reservationId] = it }
        }

        override fun confirm(reservationId: UUID): Reservation? {
            val current = reservations[reservationId] ?: return null
            return current.pay(now()).also { reservations[reservationId] = it }
        }

        override fun findExpired(at: Instant, limit: Int): List<Reservation> =
            reservations.values
                .filter { it.status == ReservationStatus.PENDING && !it.expiresAt!!.isAfter(at) }
                .take(limit)

        override fun findByUserId(userId: UUID): List<Reservation> =
            reservations.values
                .filter { it.userId == userId }
                .sortedByDescending { it.createdAt }
    }
}
