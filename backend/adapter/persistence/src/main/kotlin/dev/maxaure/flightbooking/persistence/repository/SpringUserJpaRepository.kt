package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface SpringUserJpaRepository : JpaRepository<UserEntity, UUID> {

    fun findByEmail(email: String): UserEntity?
}
