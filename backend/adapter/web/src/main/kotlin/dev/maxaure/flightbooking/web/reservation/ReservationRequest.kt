package dev.maxaure.flightbooking.web.reservation

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

internal data class HoldSeatsRequest(
    @field:NotEmpty(message = "At least one seat must be held")
    @field:Valid
    val seats: List<SeatRequest>,
)

internal data class SeatRequest(
    @field:NotBlank(message = "Seat number is required")
    val seat: String,

    @field:NotBlank(message = "Passenger name is required")
    val passengerName: String,
)
