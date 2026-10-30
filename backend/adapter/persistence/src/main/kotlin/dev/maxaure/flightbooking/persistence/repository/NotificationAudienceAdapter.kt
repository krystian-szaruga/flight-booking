package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.NotificationAudience
import dev.maxaure.flightbooking.domain.model.catalogue.Flight
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.notification.NotificationEvent
import dev.maxaure.flightbooking.domain.model.user.Email
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

internal interface ReminderRow {
    val reservationId: UUID
    val email: String
    val flightNumber: String
    val departureAt: Any
    val expiresAt: Any
    val seats: String
}

internal interface DepartureRow {
    val reservationId: UUID
    val email: String
    val flightNumber: String
    val departureAt: Any
    val seats: String
}

internal interface DisruptionRow {
    val reservationId: UUID
    val email: String
    val flightNumber: String
    val departureAt: Any
    val seats: String
}

@Transactional(readOnly = true)
internal class NotificationAudienceAdapter(
    private val reservations: SpringReservationJpaRepository,
) : NotificationAudience {

    override fun paymentReminders(
        now: Instant,
        expiresBefore: Instant,
        limit: Int,
    ): List<NotificationEvent.PaymentReminder> =
        reservations.findPaymentReminderRows(now, expiresBefore, limit).map {
            NotificationEvent.PaymentReminder(
                recipient = Email.of(it.email),
                reservationId = it.reservationId,
                flightNumber = FlightNumber.fromTrusted(it.flightNumber),
                departureAt = instant(it.departureAt),
                seats = seats(it.seats),
                expiresAt = instant(it.expiresAt),
            )
        }

    override fun departureReminders(
        now: Instant,
        departsBefore: Instant,
        limit: Int,
    ): List<NotificationEvent.DepartureReminder> =
        reservations.findDepartureReminderRows(now, departsBefore, limit).map {
            NotificationEvent.DepartureReminder(
                recipient = Email.of(it.email),
                reservationId = it.reservationId,
                flightNumber = FlightNumber.fromTrusted(it.flightNumber),
                departureAt = instant(it.departureAt),
                seats = seats(it.seats),
            )
        }

    override fun disruptedPassengers(flight: Flight): List<NotificationEvent.FlightDisrupted> =
        reservations.findDisruptedPassengers(flight.id).map {
            NotificationEvent.FlightDisrupted(
                recipient = Email.of(it.email),
                reservationId = it.reservationId,
                flightNumber = flight.flightNumber,
                departureAt = instant(it.departureAt),
                seats = seats(it.seats),
                status = flight.status,
            )
        }

    @Transactional
    override fun markPaymentReminderSent(reservationId: UUID, at: Instant) {
        reservations.markPaymentReminderSent(reservationId, at)
    }

    @Transactional
    override fun markDepartureReminderSent(reservationId: UUID, at: Instant) {
        reservations.markDepartureReminderSent(reservationId, at)
    }

    private fun seats(aggregated: String): List<SeatNumber> =
        aggregated.split(',').map(SeatNumber::of)

    private fun instant(value: Any): Instant = when (value) {
        is Instant -> value
        is OffsetDateTime -> value.toInstant()
        is Timestamp -> value.toInstant()
        is LocalDateTime -> value.toInstant(ZoneOffset.UTC)
        else -> error("Unsupported timestamp value: ${value.javaClass.name}")
    }
}
