package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.catalogue.Flight
import dev.maxaure.flightbooking.domain.model.notification.NotificationEvent
import java.time.Instant
import java.util.UUID

interface NotificationAudience {

    fun paymentReminders(now: Instant, expiresBefore: Instant, limit: Int): List<NotificationEvent.PaymentReminder>

    fun departureReminders(now: Instant, departsBefore: Instant, limit: Int): List<NotificationEvent.DepartureReminder>

    fun disruptedPassengers(flight: Flight): List<NotificationEvent.FlightDisrupted>

    fun markPaymentReminderSent(reservationId: UUID, at: Instant)

    fun markDepartureReminderSent(reservationId: UUID, at: Instant)
}
