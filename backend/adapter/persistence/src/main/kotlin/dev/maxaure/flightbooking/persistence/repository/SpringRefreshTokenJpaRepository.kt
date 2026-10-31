package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.persistence.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

internal interface SpringRefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, UUID> {

    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :at where t.id = :id and t.revokedAt is null")
    fun revoke(@Param("id") id: UUID, @Param("at") at: Instant): Int

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :at where t.familyId = :familyId and t.revokedAt is null")
    fun revokeFamily(@Param("familyId") familyId: UUID, @Param("at") at: Instant): Int

    @Modifying
    @Query("delete from RefreshTokenEntity t where t.expiresAt <= :before")
    fun deleteExpired(@Param("before") before: Instant): Int
}
