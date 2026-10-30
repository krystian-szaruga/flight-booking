package dev.maxaure.flightbooking.web.reservation

import dev.maxaure.flightbooking.application.port.`in`.ReservationView
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

internal data class ReservationResponse(
    val reservationId: String,
    val flightId: String,
    val status: String,
    val expiresAt: Instant?,
    val price: MoneyResponse?,
    val paid: PaidResponse?,
    val seats: List<HeldSeatResponse>,
) {
    companion object {
        fun from(view: ReservationView) = ReservationResponse(
            reservationId = view.reservation.id.toString(),
            flightId = view.reservation.flightId.toString(),
            status = view.reservation.status.name,
            expiresAt = view.reservation.expiresAt,
            price = view.price?.let { MoneyResponse(it.amount, it.currency) },
            paid = view.paid?.let {
                PaidResponse(it.amount.amount, it.amount.currency, it.cardLast4, it.createdAt)
            },
            seats = view.reservation.seats.map {
                HeldSeatResponse(
                    it.seatNumber.value,
                    it.firstName,
                    it.lastName,
                    it.passportNumberLast4?.let { last4 -> "••••$last4" },
                    it.phoneNumberLast4?.let { last4 -> "••••$last4" },
                    it.dateOfBirth,
                )
            },
        )
    }
}

internal data class MoneyResponse(
    val amount: BigDecimal,
    val currency: String,
)

internal data class PaidResponse(
    val amount: BigDecimal,
    val currency: String,
    val cardLast4: String,
    val paidAt: Instant,
)

internal data class HeldSeatResponse(
    val seat: String,
    val firstName: String,
    val lastName: String,
    val passportNumber: String?,
    val phoneNumber: String?,
    val dateOfBirth: LocalDate?,
)
