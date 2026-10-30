package dev.maxaure.flightbooking.mail

import dev.maxaure.flightbooking.application.port.out.NotificationPublisher
import dev.maxaure.flightbooking.domain.model.notification.NotificationEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.TimeUnit

internal class KafkaNotificationPublisher(
    private val kafka: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val topic: String,
) : NotificationPublisher {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun publish(event: NotificationEvent) {
        val payload = objectMapper.writeValueAsString(NotificationMessage.of(event))
        try {
            kafka.send(topic, event.reservationId.toString(), payload).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (ex: Exception) {
            log.error(
                "Could not publish {} for reservation {}",
                event::class.simpleName,
                event.reservationId,
                ex,
            )
        }
    }

    private companion object {
        const val SEND_TIMEOUT_SECONDS = 10L
    }
}
