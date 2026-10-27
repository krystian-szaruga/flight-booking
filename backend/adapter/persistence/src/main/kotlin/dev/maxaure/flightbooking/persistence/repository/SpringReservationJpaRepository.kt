package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.FlightSeatEntity
import dev.maxaure.flightbooking.persistence.entity.ReservationEntity
import dev.maxaure.flightbooking.persistence.entity.ReservationSeatEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

internal interface SpringReservationJpaRepository : JpaRepository<ReservationEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservationEntity r where r.id = :id")
    fun lockById(@Param("id") id: UUID): ReservationEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select s from FlightSeatEntity s
        where s.flight.id = :flightId and s.seatNumber in :seatNumbers
        order by s.seatNumber
        """,
    )
    fun lockSeats(
        @Param("flightId") flightId: UUID,
        @Param("seatNumbers") seatNumbers: Collection<String>,
    ): List<FlightSeatEntity>

    @Query(
        """
        select s from FlightSeatEntity s
        where s.id in :ids
        order by s.seatNumber
        """,
    )
    fun lockSeatsByIds(@Param("ids") ids: Collection<UUID>): List<FlightSeatEntity>

    @Query(
        """
        select r from ReservationEntity r
        where r.status = 'PENDING' and r.expiresAt <= :at
        order by r.expiresAt
        """,
    )
    fun findPendingExpired(@Param("at") at: Instant, pageable: Pageable): List<ReservationEntity>
}

internal interface SpringReservationSeatJpaRepository : JpaRepository<ReservationSeatEntity, UUID> {

    fun findByReservationId(reservationId: UUID): List<ReservationSeatEntity>

    fun deleteByReservationId(reservationId: UUID): Long
}
