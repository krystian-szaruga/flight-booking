package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.PendingRegistrationStore
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.verification.PendingRegistration
import dev.maxaure.flightbooking.persistence.mapper.PendingRegistrationMapper
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import java.sql.Timestamp
import java.time.Instant

internal class PendingRegistrationAdapter(
    private val jpa: SpringPendingRegistrationJpaRepository,
    private val entityManagerFactory: EntityManagerFactory,
) : PendingRegistrationStore {

    override fun find(email: Email): PendingRegistration? =
        jpa.findById(email.value).orElse(null)?.let(PendingRegistrationMapper::toDomain)

    override fun save(pending: PendingRegistration): PendingRegistration =
        inTransaction { em ->
            val rows = em.createNativeQuery(UPSERT)
                .setParameter("email", pending.email.value)
                .setParameter("passwordHash", pending.passwordHash.value)
                .setParameter("codeHash", pending.codeHash)
                .setParameter("expiresAt", Timestamp.from(pending.expiresAt))
                .setParameter("attempts", pending.attempts)
                .setParameter("createdAt", Timestamp.from(pending.createdAt))
                .setParameter("sentAt", Timestamp.from(pending.sentAt))
                .setParameter("sendCount", pending.sendCount)
                .resultList

            val row = rows.first() as Array<*>
            pending.copy(
                attempts = (row[0] as Number).toInt(),
                createdAt = row[1] as Instant,
                sentAt = row[2] as Instant,
                sendCount = (row[3] as Number).toInt(),
            )
        } ?: error("Upsert of pending registration ${pending.email.value} returned no row")

    override fun delete(email: Email): Boolean =
        inTransaction { em ->
            em.createNativeQuery("DELETE FROM pending_registration WHERE email = :email")
                .setParameter("email", email.value)
                .executeUpdate() > 0
        } ?: false

    override fun claimAttempt(email: Email, now: Instant): PendingRegistration? =
        inTransaction { em ->
            val rows = em.createNativeQuery(CLAIM)
                .setParameter("email", email.value)
                .setParameter("now", Timestamp.from(now))
                .setParameter("maxAttempts", PendingRegistration.MAX_ATTEMPTS)
                .resultList

            val row = rows.firstOrNull() as? Array<*> ?: return@inTransaction null
            PendingRegistration(
                email = email,
                passwordHash = PasswordHash.fromTrusted(row[0] as String),
                codeHash = row[1] as String,
                expiresAt = row[2] as Instant,
                attempts = (row[3] as Number).toInt(),
                createdAt = row[4] as Instant,
                sentAt = row[5] as Instant,
                sendCount = (row[6] as Number).toInt(),
            )
        }

    override fun purgeExpired(before: Instant): Int =
        inTransaction { em ->
            em.createNativeQuery("DELETE FROM pending_registration WHERE expires_at <= :before")
                .setParameter("before", Timestamp.from(before))
                .executeUpdate()
        } ?: 0

    private fun <T : Any> inTransaction(block: (EntityManager) -> T?): T? {
        val entityManager = entityManagerFactory.createEntityManager()
        try {
            val transaction = entityManager.transaction
            transaction.begin()
            try {
                val result = block(entityManager)
                transaction.commit()
                return result
            } catch (e: Exception) {
                if (transaction.isActive) transaction.rollback()
                throw e
            }
        } finally {
            entityManager.close()
        }
    }

    private companion object {
        val UPSERT = """
            INSERT INTO pending_registration
                (email, password_hash, code_hash, expires_at, attempts, created_at, sent_at, send_count)
            VALUES (:email, :passwordHash, :codeHash, :expiresAt, :attempts, :createdAt, :sentAt, :sendCount)
            ON CONFLICT (email) DO UPDATE
               SET password_hash = EXCLUDED.password_hash,
                   code_hash     = EXCLUDED.code_hash,
                   expires_at    = EXCLUDED.expires_at,
                   attempts      = EXCLUDED.attempts,
                   created_at    = EXCLUDED.created_at,
                   sent_at       = EXCLUDED.sent_at,
                   send_count    = EXCLUDED.send_count
            RETURNING attempts, created_at, sent_at, send_count
        """.trimIndent()

        val CLAIM = """
            UPDATE pending_registration
               SET attempts = attempts + 1
             WHERE email = :email
               AND expires_at > :now
               AND attempts < :maxAttempts
            RETURNING password_hash, code_hash, expires_at, attempts, created_at, sent_at, send_count
        """.trimIndent()
    }
}
