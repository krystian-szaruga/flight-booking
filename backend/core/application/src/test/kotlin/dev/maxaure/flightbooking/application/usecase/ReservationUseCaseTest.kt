package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.HoldOutcome
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.domain.ConflictException
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.reservation.PassengerSeat
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.reservation.ReservationStatus
import java.time.Clock
import java.time.Duration
import java.time.Instant
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
    private val useCase = ReservationUseCase(repository, clock)

    private fun seats(vararg numbers: String) =
        numbers.map { PassengerSeat(SeatNumber.of(it), "Passenger $it") }

    @Test
    fun `when a seat is already held then a second hold of the same seat is rejected and the first survives`() {
        val first = useCase.hold(flightId, seats("12A", "12B"))

        val failure = assertFailsWith<ConflictException> { useCase.hold(flightId, seats("12B", "12C")) }

        assertTrue("12B" in failure.message.orEmpty())
        assertEquals(listOf("12A", "12B"), repository.heldSeats(flightId))
        assertEquals(ReservationStatus.PENDING, repository.findById(first.id)?.status)
    }

    @Test
    fun `when a conflicting hold is rejected then none of its other seats stay held`() {
        useCase.hold(flightId, seats("12A"))

        assertFailsWith<ConflictException> { useCase.hold(flightId, seats("12C", "12A")) }

        assertEquals(listOf("12A"), repository.heldSeats(flightId))
    }

    @Test
    fun `when a hold is cancelled then its seats become available again`() {
        val hold = useCase.hold(flightId, seats("12A", "12B"))

        val cancelled = useCase.cancel(hold.id)

        assertEquals(ReservationStatus.CANCELLED, cancelled.status)
        assertNull(cancelled.expiresAt)
        assertEquals(emptyList(), repository.heldSeats(flightId))
    }

    @Test
    fun `when a hold has expired then the sweeper frees its seats and marks it expired`() {
        val stale = useCase.hold(flightId, seats("12A"))
        now = now.plus(Duration.ofMinutes(10))
        val fresh = useCase.hold(flightId, seats("12B"))
        now = now.plus(Duration.ofMinutes(6))

        val released = useCase.releaseExpired()

        assertEquals(listOf(stale.id), released)
        assertEquals(listOf("12B"), repository.heldSeats(flightId))
        assertEquals(ReservationStatus.EXPIRED, repository.findById(stale.id)?.status)
        assertEquals(ReservationStatus.PENDING, repository.findById(fresh.id)?.status)
    }

    @Test
    fun `when an expired hold is cancelled by the client then it is reported as a conflict`() {
        val stale = useCase.hold(flightId, seats("12A"))
        now = now.plus(Duration.ofMinutes(16))
        useCase.releaseExpired()

        assertFailsWith<ConflictException> { useCase.cancel(stale.id) }
    }

    @Test
    fun `when an unknown reservation is cancelled then it is reported as not found`() {
        assertFailsWith<NotFoundException> { useCase.cancel(UUID.randomUUID()) }
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

        override fun findExpired(at: Instant, limit: Int): List<Reservation> =
            reservations.values
                .filter { it.status == ReservationStatus.PENDING && !it.expiresAt!!.isAfter(at) }
                .take(limit)
    }
}
