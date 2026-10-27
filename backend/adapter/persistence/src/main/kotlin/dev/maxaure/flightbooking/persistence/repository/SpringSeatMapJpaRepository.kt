package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.FlightSeatEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

internal interface SpringSeatMapJpaRepository : JpaRepository<FlightSeatEntity, UUID> {

    @Query(
        """
        select s from FlightSeatEntity s
        join fetch s.fareClass
        where s.flight.id = :flightId
        """,
    )
    fun findByFlightId(@Param("flightId") flightId: UUID): List<FlightSeatEntity>
}
