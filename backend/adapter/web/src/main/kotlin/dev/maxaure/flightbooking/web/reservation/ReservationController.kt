package dev.maxaure.flightbooking.web.reservation

import dev.maxaure.flightbooking.application.reservation.ReservationUseCase
import dev.maxaure.flightbooking.domain.catalogue.model.SeatNumber
import dev.maxaure.flightbooking.domain.reservation.model.PassengerSeat
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/reservations")
internal class ReservationController(
    private val reservations: ReservationUseCase,
) {

    @PostMapping("/flights/{flightId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun hold(
        @PathVariable flightId: UUID,
        @RequestBody @Valid request: HoldSeatsRequest,
    ): ReservationResponse =
        ReservationResponse.from(
            reservations.hold(
                flightId = flightId,
                seats = request.seats.map { PassengerSeat(SeatNumber.of(it.seat), it.passengerName.trim()) },
            ),
        )

    @DeleteMapping("/{reservationId}")
    fun cancel(@PathVariable reservationId: UUID): ReservationResponse =
        ReservationResponse.from(reservations.cancel(reservationId))
}
