package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.notification.NotificationEvent

interface NotificationPublisher {
    fun publish(event: NotificationEvent)
}
