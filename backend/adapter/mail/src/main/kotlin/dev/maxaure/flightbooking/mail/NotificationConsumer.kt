package dev.maxaure.flightbooking.mail

import dev.maxaure.flightbooking.application.port.out.MailSender
import dev.maxaure.flightbooking.domain.model.user.Email
import org.springframework.kafka.annotation.KafkaListener
import tools.jackson.databind.ObjectMapper

internal class NotificationConsumer(
    private val mail: MailSender,
    private val objectMapper: ObjectMapper,
    private val appName: String,
) {

    @KafkaListener(topics = [$$"${notification.topic}"], groupId = $$"${notification.consumer-group}")
    fun onMessage(payload: String) {
        val message = objectMapper.readValue(payload, NotificationMessage::class.java)
        mail.send(template(message.type), Email.of(message.recipient), model(message))
    }

    private fun template(type: String): String = when (type) {
        NotificationMessage.RESERVATION_CONFIRMED -> "reservation-confirmed"
        NotificationMessage.PAYMENT_REMINDER -> "payment-reminder"
        NotificationMessage.RESERVATION_EXPIRED -> "reservation-expired"
        NotificationMessage.FLIGHT_DISRUPTED -> "flight-disrupted"
        NotificationMessage.DEPARTURE_REMINDER -> "departure-reminder"
        else -> error("Unknown notification type: $type")
    }

    private fun model(message: NotificationMessage): Map<String, String> = buildMap {
        put("appName", appName)
        put("reservationId", message.reservationId)
        put("flightNumber", message.flightNumber)
        put("departureAt", message.departureAt)
        put("seats", message.seats.joinToString(", "))
        message.expiresAt?.let { put("expiresAt", it) }
        message.status?.let { put("status", it) }
    }
}
