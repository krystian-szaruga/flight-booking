package dev.maxaure.flightbooking.mail

import dev.maxaure.flightbooking.application.port.out.MailSender
import dev.maxaure.flightbooking.domain.model.catalogue.FlightStatus
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.notification.NotificationEvent
import dev.maxaure.flightbooking.domain.model.user.Email
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationConsumerTest {

    private val mapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
    private val mail = RecordingMailSender()
    private val consumer = NotificationConsumer(mail, mapper, "flight-booking")

    @Test
    fun `when a confirmation arrives then the confirmed template is sent to the reservation owner`() {
        val event = NotificationEvent.ReservationConfirmed(
            recipient = Email.of("ada@example.com"),
            reservationId = UUID.randomUUID(),
            flightNumber = FlightNumber.of("LO381"),
            departureAt = Instant.parse("2026-11-02T08:00:00Z"),
            seats = listOf(SeatNumber.of("12A"), SeatNumber.of("12B")),
        )

        consumer.onMessage(mapper.writeValueAsString(NotificationMessage.of(event)))

        assertEquals("reservation-confirmed", mail.template)
        assertEquals("ada@example.com", mail.to?.value)
        assertEquals("LO381", mail.model?.get("flightNumber"))
        assertEquals("12A, 12B", mail.model?.get("seats"))
        assertEquals("flight-booking", mail.model?.get("appName"))
    }

    @Test
    fun `when a flight disruption arrives then the status and template match the event`() {
        val event = NotificationEvent.FlightDisrupted(
            recipient = Email.of("ada@example.com"),
            reservationId = UUID.randomUUID(),
            flightNumber = FlightNumber.of("LO381"),
            departureAt = Instant.parse("2026-11-02T08:00:00Z"),
            seats = listOf(SeatNumber.of("12A")),
            status = FlightStatus.CANCELLED,
        )

        consumer.onMessage(mapper.writeValueAsString(NotificationMessage.of(event)))

        assertEquals("flight-disrupted", mail.template)
        assertEquals("CANCELLED", mail.model?.get("status"))
    }

    @Test
    fun `when a payment reminder arrives then the expiry is rendered into the model`() {
        val expiresAt = Instant.parse("2026-10-30T10:10:00Z")
        val event = NotificationEvent.PaymentReminder(
            recipient = Email.of("ada@example.com"),
            reservationId = UUID.randomUUID(),
            flightNumber = FlightNumber.of("LO381"),
            departureAt = Instant.parse("2026-11-02T08:00:00Z"),
            seats = listOf(SeatNumber.of("12A")),
            expiresAt = expiresAt,
        )

        consumer.onMessage(mapper.writeValueAsString(NotificationMessage.of(event)))

        assertEquals("payment-reminder", mail.template)
        assertEquals(expiresAt.toString(), mail.model?.get("expiresAt"))
    }

    private class RecordingMailSender : MailSender {
        var template: String? = null
        var to: Email? = null
        var model: Map<String, String>? = null

        override fun send(template: String, to: Email, model: Map<String, String>) {
            this.template = template
            this.to = to
            this.model = model
        }
    }
}
