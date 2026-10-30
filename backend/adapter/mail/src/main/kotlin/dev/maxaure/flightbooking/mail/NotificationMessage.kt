package dev.maxaure.flightbooking.mail

import dev.maxaure.flightbooking.domain.model.notification.NotificationEvent

internal data class NotificationMessage(
    val type: String = "",
    val recipient: String = "",
    val reservationId: String = "",
    val flightNumber: String = "",
    val departureAt: String = "",
    val seats: List<String> = emptyList(),
    val expiresAt: String? = null,
    val status: String? = null,
) {
    companion object {
        const val RESERVATION_CONFIRMED = "RESERVATION_CONFIRMED"
        const val PAYMENT_REMINDER = "PAYMENT_REMINDER"
        const val RESERVATION_EXPIRED = "RESERVATION_EXPIRED"
        const val FLIGHT_DISRUPTED = "FLIGHT_DISRUPTED"
        const val DEPARTURE_REMINDER = "DEPARTURE_REMINDER"

        fun of(event: NotificationEvent): NotificationMessage = when (event) {
            is NotificationEvent.ReservationConfirmed -> common(event, RESERVATION_CONFIRMED)
            is NotificationEvent.PaymentReminder -> common(event, PAYMENT_REMINDER)
                .copy(expiresAt = event.expiresAt.toString())
            is NotificationEvent.ReservationExpired -> common(event, RESERVATION_EXPIRED)
            is NotificationEvent.FlightDisrupted -> common(event, FLIGHT_DISRUPTED)
                .copy(status = event.status.name)
            is NotificationEvent.DepartureReminder -> common(event, DEPARTURE_REMINDER)
        }

        private fun common(event: NotificationEvent, type: String): NotificationMessage =
            NotificationMessage(
                type = type,
                recipient = event.recipient.value,
                reservationId = event.reservationId.toString(),
                flightNumber = event.flightNumber.value,
                departureAt = event.departureAt.toString(),
                seats = event.seats.map { it.value },
            )
    }
}
