package dev.maxaure.flightbooking.scheduler

import dev.maxaure.flightbooking.application.port.out.PendingRegistrationStore
import org.slf4j.LoggerFactory
import java.time.Clock

internal class PendingRegistrationPurgeJob(
    private val pending: PendingRegistrationStore,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun purge() {
        val purged = pending.purgeExpired(clock.instant())
        if (purged > 0) {
            log.info("Purged {} expired pending registration(s)", purged)
        }
    }
}
