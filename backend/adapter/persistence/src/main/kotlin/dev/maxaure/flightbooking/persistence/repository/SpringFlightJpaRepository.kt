package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.FlightEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

internal interface SpringFlightJpaRepository : JpaRepository<FlightEntity, UUID> {

    fun findByFlightNumberAndDepartureAt(flightNumber: String, departureAt: Instant): FlightEntity?

    fun findByDepartureAtGreaterThanEqualAndDepartureAtLessThan(
        from: Instant,
        before: Instant,
    ): List<FlightEntity>
}
