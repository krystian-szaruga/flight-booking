package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.AirportEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface SpringAirportJpaRepository : JpaRepository<AirportEntity, UUID> {

    fun findByIataCode(iataCode: String): AirportEntity?

    fun existsByIataCode(iataCode: String): Boolean
}
