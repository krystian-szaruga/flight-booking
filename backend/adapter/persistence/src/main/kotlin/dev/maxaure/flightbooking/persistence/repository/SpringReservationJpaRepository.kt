package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.FlightSeatEntity
import dev.maxaure.flightbooking.persistence.entity.ReservationEntity
import dev.maxaure.flightbooking.persistence.entity.ReservationSeatEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
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

    @Query(
        """
        select r from ReservationEntity r
        where r.userId = :userId
        order by r.createdAt desc
        """,
    )
    fun findByUserId(@Param("userId") userId: UUID): List<ReservationEntity>

    @Query(
        value = """
        select r.id as "reservationId", u.email as "email", f.flight_number as "flightNumber",
               f.departure_at as "departureAt", r.expires_at as "expiresAt",
               string_agg(fs.seat_number, ',' order by fs.seat_number) as "seats"
        from reservation r
        join users u on u.id = r.user_id
        join flight f on f.id = r.flight_id
        join reservation_seat rs on rs.reservation_id = r.id
        join flight_seat fs on fs.id = rs.flight_seat_id
        where r.status = 'PENDING' and r.payment_reminder_sent_at is null
          and r.expires_at > :now and r.expires_at <= :expiresBefore
        group by r.id, u.email, f.flight_number, f.departure_at, r.expires_at
        order by r.expires_at
        limit :limit
        """,
        nativeQuery = true,
    )
    fun findPaymentReminderRows(
        @Param("now") now: Instant,
        @Param("expiresBefore") expiresBefore: Instant,
        @Param("limit") limit: Int,
    ): List<ReminderRow>

    @Query(
        value = """
        select r.id as "reservationId", u.email as "email", f.flight_number as "flightNumber",
               f.departure_at as "departureAt",
               string_agg(fs.seat_number, ',' order by fs.seat_number) as "seats"
        from reservation r
        join users u on u.id = r.user_id
        join flight f on f.id = r.flight_id
        join reservation_seat rs on rs.reservation_id = r.id
        join flight_seat fs on fs.id = rs.flight_seat_id
        where r.status = 'CONFIRMED' and r.departure_reminder_sent_at is null
          and f.departure_at > :now and f.departure_at <= :departsBefore
        group by r.id, u.email, f.flight_number, f.departure_at
        order by f.departure_at
        limit :limit
        """,
        nativeQuery = true,
    )
    fun findDepartureReminderRows(
        @Param("now") now: Instant,
        @Param("departsBefore") departsBefore: Instant,
        @Param("limit") limit: Int,
    ): List<DepartureRow>

    @Query(
        value = """
        select r.id as "reservationId", u.email as "email", f.flight_number as "flightNumber",
               f.departure_at as "departureAt",
               string_agg(fs.seat_number, ',' order by fs.seat_number) as "seats"
        from reservation r
        join users u on u.id = r.user_id
        join flight f on f.id = r.flight_id
        join reservation_seat rs on rs.reservation_id = r.id
        join flight_seat fs on fs.id = rs.flight_seat_id
        where r.flight_id = :flightId and r.status in ('PENDING', 'CONFIRMED')
        group by r.id, u.email, f.flight_number, f.departure_at
        """,
        nativeQuery = true,
    )
    fun findDisruptedPassengers(@Param("flightId") flightId: UUID): List<DisruptionRow>

    @Modifying
    @Query(
        "update ReservationEntity r set r.paymentReminderSentAt = :at " +
            "where r.id = :id and r.paymentReminderSentAt is null",
    )
    fun markPaymentReminderSent(@Param("id") id: UUID, @Param("at") at: Instant): Int

    @Modifying
    @Query(
        "update ReservationEntity r set r.departureReminderSentAt = :at " +
            "where r.id = :id and r.departureReminderSentAt is null",
    )
    fun markDepartureReminderSent(@Param("id") id: UUID, @Param("at") at: Instant): Int
}

internal interface SpringReservationSeatJpaRepository : JpaRepository<ReservationSeatEntity, UUID> {

    fun findByReservationId(reservationId: UUID): List<ReservationSeatEntity>

    fun deleteByReservationId(reservationId: UUID): Long

    @Query(
        value = """
        select sum(fc.base_price_amount * fc.multiplier) as amount, max(fc.currency) as currency
        from reservation_seat rs
        join flight_seat fs on fs.id = rs.flight_seat_id
        join fare_class fc on fc.id = fs.fare_class_id
        where rs.reservation_id = :reservationId
        """,
        nativeQuery = true,
    )
    fun sumPriceForReservation(@Param("reservationId") reservationId: UUID): ReservationPriceView?
}
