package dev.maxaure.flightbooking.domain.model.notification

import dev.maxaure.flightbooking.domain.model.catalogue.FlightStatus
import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.user.Email
import java.time.Instant
import java.util.UUID

sealed interface NotificationEvent {
    val recipient: Email
    val reservationId: UUID
    val flightNumber: FlightNumber
    val departureAt: Instant
    val seats: List<SeatNumber>

    data class ReservationConfirmed(
        override val recipient: Email,
        override val reservationId: UUID,
        override val flightNumber: FlightNumber,
        override val departureAt: Instant,
        override val seats: List<SeatNumber>,
    ) : NotificationEvent

    data class PaymentReminder(
        override val recipient: Email,
        override val reservationId: UUID,
        override val flightNumber: FlightNumber,
        override val departureAt: Instant,
        override val seats: List<SeatNumber>,
        val expiresAt: Instant,
    ) : NotificationEvent

    data class ReservationExpired(
        override val recipient: Email,
        override val reservationId: UUID,
        override val flightNumber: FlightNumber,
        override val departureAt: Instant,
        override val seats: List<SeatNumber>,
    ) : NotificationEvent

    data class FlightDisrupted(
        override val recipient: Email,
        override val reservationId: UUID,
        override val flightNumber: FlightNumber,
        override val departureAt: Instant,
        override val seats: List<SeatNumber>,
        val status: FlightStatus,
    ) : NotificationEvent {
        init {
            require(status == FlightStatus.DELAYED || status == FlightStatus.CANCELLED) {
                "A flight disruption must be a delay or a cancellation"
            }
        }
    }

    data class DepartureReminder(
        override val recipient: Email,
        override val reservationId: UUID,
        override val flightNumber: FlightNumber,
        override val departureAt: Instant,
        override val seats: List<SeatNumber>,
    ) : NotificationEvent
}
