package dev.maxaure.flightbooking.scheduler

import dev.maxaure.flightbooking.application.port.`in`.ReserveSeats
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration
@EnableScheduling
@ConditionalOnProperty("reservation.expiry.enabled", havingValue = "true", matchIfMissing = true)
internal class ReservationExpiryConfig {

    @Bean
    fun reservationExpiryJob(reservations: ReserveSeats) = ReservationExpiryJob(reservations)

    @Bean
    fun reservationExpiryTrigger(job: ReservationExpiryJob) = ReservationExpiryTrigger(job)
}

internal class ReservationExpiryTrigger(private val job: ReservationExpiryJob) {

    @Scheduled(cron = $$"${reservation.expiry.cron}")
    fun onSchedule() = job.sweep()
}
