package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.FlightEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

internal interface SpringFlightSearchJpaRepository : JpaRepository<FlightEntity, UUID> {

    @Query(
        """
        select distinct f from FlightEntity f
        left join fetch f.fares
        join f.origin o
        join f.destination d
        where o.iataCode = :origin
          and d.iataCode = :destination
          and f.departureAt >= :from
          and f.departureAt < :until
        order by f.departureAt, f.flightNumber
        """,
    )
    fun findByRouteAndDay(
        @Param("origin") origin: String,
        @Param("destination") destination: String,
        @Param("from") from: Instant,
        @Param("until") until: Instant,
    ): List<FlightEntity>

    @Query(
        """
        select s.flight.id as flightId, s.fareClass.cabin as cabin, count(s) as available
        from FlightSeatEntity s
        where s.flight.id in :flightIds
          and s.status = 'AVAILABLE'
        group by s.flight.id, s.fareClass.cabin
        """,
    )
    fun countAvailableByCabin(@Param("flightIds") flightIds: List<UUID>): List<AvailableSeatsByCabin>

    @Query(
        """
        select f.id as flightId,
               f.flightNumber as flightNumber,
               o.iataCode as origin,
               d.iataCode as destination,
               f.departureAt as departureAt,
               f.arrivalAt as arrivalAt,
               fc.cabin as cabin,
               fc.basePriceAmount * fc.multiplier as priceAmount,
               fc.currency as currency,
               count(s) as available
        from FlightSeatEntity s
        join s.flight f
        join f.origin o
        join f.destination d
        join s.fareClass fc
        where s.status = 'AVAILABLE'
          and f.status <> 'CANCELLED'
          and f.departureAt >= :from
        group by f.id, f.flightNumber, o.iataCode, d.iataCode, f.departureAt, f.arrivalAt,
                 fc.cabin, fc.basePriceAmount, fc.multiplier, fc.currency
        order by priceAmount asc, f.departureAt asc
        """,
    )
    fun findDeals(
        @Param("from") from: Instant,
        pageable: Pageable,
    ): List<DealRow>
}

internal interface DealRow {
    val flightId: UUID
    val flightNumber: String
    val origin: String
    val destination: String
    val departureAt: Instant
    val arrivalAt: Instant
    val cabin: String
    val priceAmount: BigDecimal
    val currency: String
    val available: Long
}

internal interface AvailableSeatsByCabin {
    val flightId: UUID
    val cabin: String
    val available: Long
}
