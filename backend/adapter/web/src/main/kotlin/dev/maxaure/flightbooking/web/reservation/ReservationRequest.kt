package dev.maxaure.flightbooking.web.reservation

import dev.maxaure.flightbooking.application.port.`in`.HoldSeatRequest
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.reservation.PassengerType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

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

    @field:NotBlank(message = "Passport number is required")
    val passportNumber: String? = null,

    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(
        regexp = "^\\+[1-9][0-9]{6,14}$",
        message = "Phone number must start with + and contain only digits",
    )
    val phoneNumber: String? = null,

    @field:NotNull(message = "Date of birth is required")
    @field:Past(message = "Date of birth must be in the past")
    val dateOfBirth: LocalDate? = null,
) {
    fun toRequest(): HoldSeatRequest =
        HoldSeatRequest(
            seatNumber = SeatNumber.of(seat),
            passengerType = requireNotNull(passengerType) { "Passenger type is required" },
            firstName = firstName,
            lastName = lastName,
            passportNumber = passportNumber,
            phoneNumber = phoneNumber,
            dateOfBirth = dateOfBirth,
        )

    override fun toString(): String =
        "SeatRequest(seat=$seat, passengerType=$passengerType, firstName=$firstName, lastName=$lastName, passportNumber=${passportNumber?.let { "••••${it.takeLast(4)}" } ?: "none"}, phoneNumber=${phoneNumber?.let { "••••${it.takeLast(4)}" } ?: "none"}, dateOfBirth=$dateOfBirth)"
}

internal data class CancelSeatsRequest(
    @field:NotEmpty(message = "Pick at least one seat to release")
    val seats: List<String>,
)
