package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.PendingRegistrationEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface SpringPendingRegistrationJpaRepository : JpaRepository<PendingRegistrationEntity, String>
