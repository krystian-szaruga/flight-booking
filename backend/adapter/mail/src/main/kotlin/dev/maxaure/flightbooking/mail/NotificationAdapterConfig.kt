package dev.maxaure.flightbooking.mail

import dev.maxaure.flightbooking.application.port.out.MailSender
import dev.maxaure.flightbooking.application.port.out.NotificationPublisher
import dev.maxaure.flightbooking.domain.model.notification.NotificationEvent
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import tools.jackson.databind.ObjectMapper

@Configuration
internal class NotificationAdapterConfig {

    @Bean
    @ConditionalOnProperty("notification.kafka.enabled", havingValue = "true")
    fun notificationTopic(@Value($$"${notification.topic}") topic: String): NewTopic =
        NewTopic(topic, 1, 1)

    @Bean
    @ConditionalOnProperty("notification.kafka.enabled", havingValue = "true")
    fun kafkaNotificationPublisher(
        kafka: KafkaTemplate<String, String>,
        objectMapper: ObjectMapper,
        @Value($$"${notification.topic}") topic: String,
    ): NotificationPublisher = KafkaNotificationPublisher(kafka, objectMapper, topic)

    @Bean
    @ConditionalOnProperty("notification.kafka.enabled", havingValue = "true")
    fun notificationConsumer(
        mail: MailSender,
        objectMapper: ObjectMapper,
        @Value($$"${app.mail.app-name}") appName: String,
    ): NotificationConsumer = NotificationConsumer(mail, objectMapper, appName)

    @Bean
    @ConditionalOnProperty("notification.kafka.enabled", havingValue = "false")
    fun noopNotificationPublisher(): NotificationPublisher = NoopNotificationPublisher
}

internal object NoopNotificationPublisher : NotificationPublisher {
    override fun publish(event: NotificationEvent) = Unit
}
