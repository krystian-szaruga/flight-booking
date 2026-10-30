package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.HoldOutcome
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.reservation.PassengerSeat
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.reservation.ReservationStatus
import dev.maxaure.flightbooking.domain.model.user.UserId
import java.time.Instant
import java.time.Duration
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListMyReservationsUseCaseTest {

    private val now = Instant.parse("2026-10-27T10:00:00Z")
    private val repository = InMemoryReservationRepository()
    private val useCase = ListMyReservationsUseCase(repository)

    private val alice = UserId.new()
    private val bob = UserId.new()
    private val flightId = UUID.randomUUID()

    @Test
    fun `when two users hold seats then each listing contains only the caller's reservations`() {
        val aliceHold = repository.add(alice, flightId, "12A", now)
        val bobHold = repository.add(bob, flightId, "12B", now)

        val aliceList = useCase.list(alice)
        val bobList = useCase.list(bob)

        assertEquals(listOf(aliceHold.id), aliceList.map { it.id })
        assertEquals(listOf(bobHold.id), bobList.map { it.id })
    }

    @Test
    fun `when a user has several reservations then they are returned newest first regardless of status`() {
        val oldest = repository.add(alice, flightId, "12A", now.minus(Duration.ofMinutes(30)))
        val middle = repository.add(alice, flightId, "12B", now.minus(Duration.ofMinutes(20)))
            .let { repository.cancel(it.id) }
        val newest = repository.add(alice, flightId, "12C", now.minus(Duration.ofMinutes(10)))

        val listed = useCase.list(alice)

        assertEquals(listOf(newest.id, middle.id, oldest.id), listed.map { it.id })
        assertEquals(ReservationStatus.CANCELLED, listed[1].status)
    }

    @Test
    fun `when a reservation was cancelled and released its seats then the rest of the listing still loads`() {
        val cancelled = repository.add(alice, flightId, "12A", now).let { repository.cancel(it.id) }.let {
            repository.forgetSeats(it.id)
        }
        val pending = repository.add(alice, flightId, "12B", now)

        val listed = useCase.list(alice)

        assertEquals(listOf(pending.id), listed.map { it.id })
        assertEquals(ReservationStatus.CANCELLED, repository.findById(cancelled.id)?.status)
    }

    @Test
    fun `when a user has no reservations then the listing is empty`() {
        repository.add(bob, flightId, "12A", now)

        assertTrue(useCase.list(alice).isEmpty())
    }

    private class InMemoryReservationRepository : ReservationRepository {
        private val stored = linkedMapOf<UUID, Reservation>()

        fun add(userId: UserId, flightId: UUID, seat: String, createdAt: Instant): Reservation {
            val reservation = Reservation(
                id = UUID.randomUUID(),
                flightId = flightId,
                userId = userId.value,
                status = ReservationStatus.PENDING,
                expiresAt = createdAt.plus(Duration.ofMinutes(15)),
                createdAt = createdAt,
                seats = listOf(PassengerSeat(SeatNumber.of(seat), "Ada", "Lovelace")),
            )
            stored[reservation.id] = reservation
            return reservation
        }

        fun cancel(id: UUID): Reservation {
            val cancelled = stored.getValue(id).copy(status = ReservationStatus.CANCELLED, expiresAt = null)
            stored[id] = cancelled
            return cancelled
        }

        fun forgetSeats(id: UUID): Reservation {
            val released = stored.getValue(id).copy(seats = emptyList())
            stored[id] = released
            return released
        }

        override fun findById(id: UUID): Reservation? = stored[id]

        override fun save(reservation: Reservation): Reservation = reservation.also { stored[it.id] = it }

        override fun holdSeats(reservation: Reservation): HoldOutcome = error("Not used by the listing")

        override fun releaseSeats(reservationId: UUID): Reservation? = error("Not used by the listing")

        override fun releaseSeatSubset(reservationId: UUID, seatNumbers: List<String>): Reservation? =
            error("Not used by these tests")

        override fun confirm(reservationId: UUID): Reservation? = error("Not used by these tests")

        override fun findExpired(at: Instant, limit: Int): List<Reservation> =
            stored.values.filter { it.status == ReservationStatus.PENDING && !it.expiresAt!!.isAfter(at) }.take(limit)

        override fun findByUserId(userId: UUID): List<Reservation> =
            stored.values.filter { it.userId == userId }.sortedByDescending { it.createdAt }
    }
}
