package dev.maxaure.flightbooking.domain.model.reservation

import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.ConflictException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReservationTest {

    private val now = Instant.parse("2026-10-27T10:00:00Z")
    private val flightId = UUID.randomUUID()
    private val seats = listOf(
        PassengerSeat(SeatNumber.of("12A"), "Ada Lovelace"),
        PassengerSeat(SeatNumber.of("12B"), "Alan Turing"),
    )

    @Test
    fun `when a hold is placed then every seat is pending until the hold duration runs out`() {
        val hold = Reservation.hold(flightId, seats, now)

        assertEquals(ReservationStatus.PENDING, hold.status)
        assertEquals(now.plus(Duration.ofMinutes(15)), hold.expiresAt)
        assertEquals(seats, hold.seats)
        assertNull(hold.userId)
    }

    @Test
    fun `when an expired hold is cancelled then it is rejected`() {
        val expired = Reservation.hold(flightId, seats, now).expire(now.plus(Duration.ofMinutes(16)))

        assertFailsWith<ConflictException> { expired.cancel() }
    }

    @Test
    fun `when a reservation holds the same seat twice then it is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            Reservation.hold(flightId, listOf(seats[0], seats[0]), now)
        }
    }
}
