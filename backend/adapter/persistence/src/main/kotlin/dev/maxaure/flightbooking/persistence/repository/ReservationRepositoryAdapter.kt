package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.HoldOutcome
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.reservation.PassengerSeat
import dev.maxaure.flightbooking.domain.model.reservation.Reservation
import dev.maxaure.flightbooking.domain.model.reservation.ReservationStatus
import dev.maxaure.flightbooking.persistence.entity.FlightSeatEntity
import dev.maxaure.flightbooking.persistence.entity.ReservationEntity
import dev.maxaure.flightbooking.persistence.mapper.ReservationMapper
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.UUID

internal class ReservationRepositoryAdapter(
    private val reservations: SpringReservationJpaRepository,
    private val reservationSeats: SpringReservationSeatJpaRepository,
    private val transactions: TransactionTemplate,
) : ReservationRepository {

    override fun findById(id: UUID): Reservation? = transactions.execute { load(id) }

    override fun save(reservation: Reservation): Reservation =
        transactions.execute {
            val saved = when (val existing = reservations.findById(reservation.id).orElse(null)) {
                null -> reservations.save(ReservationMapper.toEntity(reservation))
                else -> existing.apply {
                    status = reservation.status.name
                    expiresAt = reservation.expiresAt
                }
            }
            ReservationMapper.toDomain(saved, reservation.seats)
        }

    override fun holdSeats(reservation: Reservation): HoldOutcome = transactions.execute {
        val wanted = reservation.seats.map { it.seatNumber.value }
        val locked = reservations.lockSeats(reservation.flightId, wanted)
        val byNumber = locked.associateBy { it.seatNumber }
        val unavailable = wanted.filter { byNumber[it]?.status != AVAILABLE }
        if (unavailable.isNotEmpty()) return@execute HoldOutcome.SeatUnavailable(unavailable)

        reservations.save(ReservationMapper.toEntity(reservation))
        wanted.forEach { number ->
            val seat = byNumber.getValue(number)
            seat.status = HELD
            seat.holdExpiresAt = reservation.expiresAt
            seat.reservationId = reservation.id
        }
        reservationSeats.saveAll(
            reservation.seats.map { seat ->
                ReservationMapper.seatToEntity(
                    reservationId = reservation.id,
                    flightSeatId = byNumber.getValue(seat.seatNumber.value).id,
                    seat = seat,
                )
            },
        )
        HoldOutcome.Held(reservation)
    }

    override fun releaseSeats(reservationId: UUID): Reservation? = transactions.execute {
        val locked = reservations.lockById(reservationId) ?: return@execute null
        val current = load(reservationId) ?: return@execute null
        val released = when (current.status) {
            ReservationStatus.PENDING ->
                if (isHoldOver(locked)) current.expire(clockNow()) else current.cancel()
            else -> current
        }
        if (released.status == current.status) return@execute released

        locked.status = released.status.name
        locked.expiresAt = null
        releaseHeldSeats(reservationId)
        released
    }

    override fun findExpired(at: Instant, limit: Int): List<Reservation> = transactions.execute {
        reservations.findPendingExpired(at, PageRequest.of(0, limit))
            .mapNotNull { entity -> load(entity.id) }
    }

    private fun load(id: UUID): Reservation? {
        val entity = reservations.findById(id).orElse(null) ?: return null
        val numbers = heldSeatNumbers(id)
        return ReservationMapper.toDomain(entity, entity.seats(numbers))
    }

    private fun ReservationEntity.seats(numbers: Map<UUID, String>): List<PassengerSeat> =
        reservationSeats.findByReservationId(id)
            .mapNotNull { link ->
                numbers[link.flightSeatId]?.let { number ->
                    ReservationMapper.seatToDomain(link, SeatNumber.of(number))
                }
            }

    private fun heldSeatNumbers(reservationId: UUID): Map<UUID, String> {
        val linkIds = reservationSeats.findByReservationId(reservationId).map { it.flightSeatId }
        if (linkIds.isEmpty()) return emptyMap()
        return reservations.lockSeatsByIds(linkIds).associate { it.id to it.seatNumber }
    }

    private fun releaseHeldSeats(reservationId: UUID) {
        val seatIds = reservationSeats.findByReservationId(reservationId).map { it.flightSeatId }
        if (seatIds.isEmpty()) return
        reservations.lockSeatsByIds(seatIds).forEach { it.release() }
        reservationSeats.deleteByReservationId(reservationId)
    }

    private fun isHoldOver(seat: ReservationEntity): Boolean =
        seat.expiresAt?.let { !it.isAfter(clockNow()) } ?: true

    private fun clockNow(): Instant = Instant.now()

    private fun FlightSeatEntity.release() {
        status = AVAILABLE
        holdExpiresAt = null
        reservationId = null
    }

    private companion object {
        const val AVAILABLE = "AVAILABLE"
        const val HELD = "HELD"
    }
}
