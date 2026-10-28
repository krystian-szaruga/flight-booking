package dev.maxaure.flightbooking.scheduler

import dev.maxaure.flightbooking.application.port.out.RevokedTokenRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.time.Clock

@Configuration
@EnableScheduling
@ConditionalOnProperty("app.auth.revoked-token.purge-enabled", havingValue = "true", matchIfMissing = true)
internal class TokenPurgeConfig {

    @Bean
    fun tokenPurgeJob(revokedTokens: RevokedTokenRepository, clock: Clock) =
        TokenPurgeJob(revokedTokens, clock)

    @Bean
    fun tokenPurgeTrigger(job: TokenPurgeJob) = TokenPurgeTrigger(job)
}

internal class TokenPurgeTrigger(private val job: TokenPurgeJob) {

    @Scheduled(cron = $$"${app.auth.revoked-token.purge-cron}")
    fun onSchedule() = job.purge()
}
