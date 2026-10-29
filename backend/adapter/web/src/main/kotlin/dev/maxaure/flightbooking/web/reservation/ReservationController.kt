package dev.maxaure.flightbooking.web.reservation

import dev.maxaure.flightbooking.application.port.`in`.ListMyReservations
import dev.maxaure.flightbooking.application.port.`in`.ReserveSeats
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.user.UserId
import dev.maxaure.flightbooking.web.auth.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
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
    private val reservations: ReserveSeats,
    private val myReservations: ListMyReservations,
) {

    @GetMapping
    fun list(@CurrentUser userId: UserId): List<ReservationResponse> =
        myReservations.list(userId).map(ReservationResponse::from)

    @PostMapping("/flights/{flightId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun hold(
        @CurrentUser userId: UserId,
        @PathVariable flightId: UUID,
        @RequestBody @Valid request: HoldSeatsRequest,
    ): ReservationResponse =
        ReservationResponse.from(
            reservations.hold(
                userId = userId,
                flightId = flightId,
                seats = request.seats.map { it.toRequest() },
            ),
        )

    @DeleteMapping("/{reservationId}")
    fun cancel(
        @CurrentUser userId: UserId,
        @PathVariable reservationId: UUID,
    ): ReservationResponse =
        ReservationResponse.from(reservations.cancel(userId, reservationId))

    @DeleteMapping("/{reservationId}/seats")
    fun cancelSeats(
        @CurrentUser userId: UserId,
        @PathVariable reservationId: UUID,
        @RequestBody @Valid request: CancelSeatsRequest,
    ): ReservationResponse =
        ReservationResponse.from(
            reservations.cancelSeats(userId, reservationId, request.seats.map(SeatNumber::of)),
        )
}
