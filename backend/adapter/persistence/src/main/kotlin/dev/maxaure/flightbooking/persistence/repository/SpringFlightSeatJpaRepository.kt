package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.FlightSeatEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface SpringFlightSeatJpaRepository : JpaRepository<FlightSeatEntity, UUID> {

    fun countByFlightId(flightId: UUID): Long

    fun findByFlightIdOrderBySeatNumber(flightId: UUID): List<FlightSeatEntity>

    fun deleteByFlightId(flightId: UUID): Long
}
