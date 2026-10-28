package dev.maxaure.flightbooking.scheduler

import dev.maxaure.flightbooking.application.port.out.RevokedTokenRepository
import org.slf4j.LoggerFactory
import java.time.Clock

internal class TokenPurgeJob(
    private val revokedTokens: RevokedTokenRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun purge() {
        val purged = revokedTokens.purgeExpired(clock.instant())
        if (purged > 0) {
            log.info("Purged {} expired revoked token(s)", purged)
        }
    }
}
