package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.SendDueNotifications
import dev.maxaure.flightbooking.application.port.out.NotificationAudience
import dev.maxaure.flightbooking.application.port.out.NotificationPublisher
import java.time.Clock
import java.time.Duration

class SendDueNotificationsUseCase(
    private val audience: NotificationAudience,
    private val publisher: NotificationPublisher,
    private val clock: Clock,
    private val paymentReminderLead: Duration,
    private val departureReminderLead: Duration,
    private val batchSize: Int = 200,
) : SendDueNotifications {

    override fun sendPaymentReminders(): Int {
        val now = clock.instant()
        val due = audience.paymentReminders(now, now.plus(paymentReminderLead), batchSize)
        due.forEach {
            publisher.publish(it)
            audience.markPaymentReminderSent(it.reservationId, now)
        }
        return due.size
    }

    override fun sendDepartureReminders(): Int {
        val now = clock.instant()
        val due = audience.departureReminders(now, now.plus(departureReminderLead), batchSize)
        due.forEach {
            publisher.publish(it)
            audience.markDepartureReminderSent(it.reservationId, now)
        }
        return due.size
    }
}
