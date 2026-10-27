package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.IngestionRunEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface SpringIngestionRunJpaRepository : JpaRepository<IngestionRunEntity, UUID>
