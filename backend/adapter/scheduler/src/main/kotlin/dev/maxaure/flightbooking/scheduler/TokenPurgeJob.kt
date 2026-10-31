package dev.maxaure.flightbooking.scheduler

import dev.maxaure.flightbooking.application.port.out.RefreshTokenStore
import dev.maxaure.flightbooking.application.port.out.RevokedTokenRepository
import org.slf4j.LoggerFactory
import java.time.Clock

internal class TokenPurgeJob(
    private val revokedTokens: RevokedTokenRepository,
    private val refreshTokens: RefreshTokenStore,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun purge() {
        val purged = revokedTokens.purgeExpired(clock.instant())
        if (purged > 0) {
            log.info("Purged {} expired revoked token(s)", purged)
        }
        val purgedRefresh = refreshTokens.purgeExpired(clock.instant())
        if (purgedRefresh > 0) {
            log.info("Purged {} expired refresh token(s)", purgedRefresh)
        }
    }
}
