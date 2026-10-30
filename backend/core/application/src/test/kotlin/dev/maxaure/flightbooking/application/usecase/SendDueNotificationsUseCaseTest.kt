package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.out.NotificationAudience
import dev.maxaure.flightbooking.application.port.out.NotificationPublisher
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.notification.NotificationEvent
import dev.maxaure.flightbooking.domain.model.user.Email
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SendDueNotificationsUseCaseTest {

    private val now = Instant.parse("2026-10-30T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val payment = NotificationEvent.PaymentReminder(
        recipient = Email.of("ada@example.com"),
        reservationId = UUID.randomUUID(),
        flightNumber = FlightNumber.of("LO381"),
        departureAt = now.plus(Duration.ofDays(2)),
        seats = listOf(SeatNumber.of("12A")),
        expiresAt = now.plus(Duration.ofMinutes(5)),
    )
    private val departure = NotificationEvent.DepartureReminder(
        recipient = Email.of("ada@example.com"),
        reservationId = UUID.randomUUID(),
        flightNumber = FlightNumber.of("LO381"),
        departureAt = now.plus(Duration.ofHours(12)),
        seats = listOf(SeatNumber.of("12A")),
    )
    private val audience = FakeAudience(payment = payment, departure = departure)
    private val publisher = RecordingPublisher()
    private val useCase = SendDueNotificationsUseCase(
        audience,
        publisher,
        clock,
        Duration.ofMinutes(10),
        Duration.ofHours(24),
    )

    @Test
    fun `when a hold expires inside the reminder window then its owner is reminded and the hold is marked`() {
        val sent = useCase.sendPaymentReminders()

        assertEquals(1, sent)
        assertEquals(payment, publisher.events.single())
        assertEquals(listOf(payment.reservationId), audience.paymentMarked)
        assertEquals(now.plus(Duration.ofMinutes(10)), audience.paymentWindowEnd)
    }

    @Test
    fun `when a hold expires after the reminder window then no payment reminder is sent`() {
        audience.paymentCandidates = listOf(payment.copy(expiresAt = now.plus(Duration.ofHours(1))))

        val sent = useCase.sendPaymentReminders()

        assertEquals(0, sent)
        assertEquals(0, publisher.events.size)
        assertEquals(0, audience.paymentMarked.size)
    }

    @Test
    fun `when publishing fails then the reminder stays unmarked so the next sweep retries`() {
        publisher.failure = IllegalStateException("kafka is down")

        assertTrue(runCatching { useCase.sendPaymentReminders() }.isFailure)
        assertEquals(0, audience.paymentMarked.size)
    }

    @Test
    fun `when a confirmed flight departs inside the reminder window then its owner is reminded`() {
        val sent = useCase.sendDepartureReminders()

        assertEquals(1, sent)
        assertEquals(departure, publisher.events.single())
        assertEquals(listOf(departure.reservationId), audience.departureMarked)
    }

    private class RecordingPublisher : NotificationPublisher {
        val events = mutableListOf<NotificationEvent>()
        var failure: RuntimeException? = null

        override fun publish(event: NotificationEvent) {
            failure?.let { throw it }
            events += event
        }
    }

    private class FakeAudience(
        var payment: NotificationEvent.PaymentReminder? = null,
        var departure: NotificationEvent.DepartureReminder? = null,
    ) : NotificationAudience {
        var paymentCandidates = listOfNotNull(payment)
        var departureCandidates = listOfNotNull(departure)
        var paymentWindowEnd: Instant? = null
        var paymentMarked = mutableListOf<UUID>()
        var departureMarked = mutableListOf<UUID>()

        override fun paymentReminders(
            now: Instant,
            expiresBefore: Instant,
            limit: Int,
        ): List<NotificationEvent.PaymentReminder> {
            paymentWindowEnd = expiresBefore
            return paymentCandidates
                .filter { it.expiresAt.isAfter(now) && !it.expiresAt.isAfter(expiresBefore) }
                .take(limit)
        }

        override fun departureReminders(
            now: Instant,
            departsBefore: Instant,
            limit: Int,
        ): List<NotificationEvent.DepartureReminder> =
            departureCandidates
                .filter { it.departureAt.isAfter(now) && !it.departureAt.isAfter(departsBefore) }
                .take(limit)

        override fun disruptedPassengers(
            flight: dev.maxaure.flightbooking.domain.model.catalogue.Flight,
        ): List<NotificationEvent.FlightDisrupted> = emptyList()

        override fun markPaymentReminderSent(reservationId: UUID, at: Instant) {
            paymentMarked += reservationId
        }

        override fun markDepartureReminderSent(reservationId: UUID, at: Instant) {
            departureMarked += reservationId
        }
    }
}
