package dev.maxaure.flightbooking.scheduler

import dev.maxaure.flightbooking.application.port.`in`.SendDueNotifications
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled

internal class PaymentReminderJob(private val notifications: SendDueNotifications) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun send() {
        val sent = notifications.sendPaymentReminders()
        if (sent > 0) log.info("Sent {} payment reminder(s)", sent)
    }
}

internal class DepartureReminderJob(private val notifications: SendDueNotifications) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun send() {
        val sent = notifications.sendDepartureReminders()
        if (sent > 0) log.info("Sent {} departure reminder(s)", sent)
    }
}

@Configuration
@ConditionalOnProperty("notification.payment-reminder.enabled", havingValue = "true")
internal class PaymentReminderConfig {

    @Bean
    fun paymentReminderJob(notifications: SendDueNotifications) = PaymentReminderJob(notifications)

    @Bean
    fun paymentReminderTrigger(job: PaymentReminderJob) = PaymentReminderTrigger(job)
}

internal class PaymentReminderTrigger(private val job: PaymentReminderJob) {

    @Scheduled(cron = $$"${notification.payment-reminder.cron}")
    fun onSchedule() = job.send()
}

@Configuration
@ConditionalOnProperty("notification.departure-reminder.enabled", havingValue = "true")
internal class DepartureReminderConfig {

    @Bean
    fun departureReminderJob(notifications: SendDueNotifications) = DepartureReminderJob(notifications)

    @Bean
    fun departureReminderTrigger(job: DepartureReminderJob) = DepartureReminderTrigger(job)
}

internal class DepartureReminderTrigger(private val job: DepartureReminderJob) {

    @Scheduled(cron = $$"${notification.departure-reminder.cron}")
    fun onSchedule() = job.send()
}
