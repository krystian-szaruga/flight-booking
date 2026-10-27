package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.FareClassEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface SpringFareClassJpaRepository : JpaRepository<FareClassEntity, UUID> {

    fun findByFlightId(flightId: UUID): List<FareClassEntity>
}
