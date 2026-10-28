package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.RevokedTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

internal interface SpringRevokedTokenJpaRepository : JpaRepository<RevokedTokenEntity, UUID> {

    @Modifying
    @Query("delete from RevokedTokenEntity t where t.expiresAt <= :before")
    fun deleteExpired(@Param("before") before: Instant): Int
}
