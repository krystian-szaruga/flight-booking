package dev.maxaure.flightbooking.web.reservation

import dev.maxaure.flightbooking.application.port.`in`.HoldSeatRequest
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.reservation.PassengerType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

internal data class HoldSeatsRequest(
    @field:NotEmpty(message = "At least one seat must be held")
    @field:Valid
    val seats: List<SeatRequest>,
)

internal data class SeatRequest(
    @field:NotBlank(message = "Seat number is required")
    val seat: String,

    @field:NotNull(message = "Say whether the seat is for you or someone else")
    val passengerType: PassengerType?,

    val firstName: String? = null,

    val lastName: String? = null,
) {
    fun toRequest(): HoldSeatRequest =
        HoldSeatRequest(
            seatNumber = SeatNumber.of(seat),
            passengerType = requireNotNull(passengerType) { "Passenger type is required" },
            firstName = firstName,
            lastName = lastName,
        )
}

internal data class CancelSeatsRequest(
    @field:NotEmpty(message = "Pick at least one seat to release")
    val seats: List<String>,
)
