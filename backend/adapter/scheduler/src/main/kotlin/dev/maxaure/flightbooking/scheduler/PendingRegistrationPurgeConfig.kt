package dev.maxaure.flightbooking.scheduler

import dev.maxaure.flightbooking.application.port.out.PendingRegistrationStore
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.Clock

@Configuration
@EnableScheduling
@ConditionalOnProperty("app.auth.verification.purge-enabled", havingValue = "true", matchIfMissing = true)
internal class PendingRegistrationPurgeConfig {

    @Bean
    fun pendingRegistrationPurgeJob(pending: PendingRegistrationStore, clock: Clock) =
        PendingRegistrationPurgeJob(pending, clock)

    @Bean
    fun pendingRegistrationPurgeTrigger(job: PendingRegistrationPurgeJob) =
        PendingRegistrationPurgeTrigger(job)
}

internal class PendingRegistrationPurgeTrigger(private val job: PendingRegistrationPurgeJob) {

    @Scheduled(cron = $$"${app.auth.verification.purge-cron}")
    fun onSchedule() = job.purge()
}
