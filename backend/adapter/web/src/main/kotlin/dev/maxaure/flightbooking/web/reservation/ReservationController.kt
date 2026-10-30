package dev.maxaure.flightbooking.web.reservation

import dev.maxaure.flightbooking.application.port.`in`.DescribeReservation
import dev.maxaure.flightbooking.application.port.`in`.ListMyReservations
import dev.maxaure.flightbooking.application.port.`in`.PayReservation
import dev.maxaure.flightbooking.application.port.`in`.ReserveSeats
import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import dev.maxaure.flightbooking.domain.model.payment.PaymentStatus
import dev.maxaure.flightbooking.domain.model.user.UserId
import dev.maxaure.flightbooking.web.auth.CurrentUser
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/reservations")
internal class ReservationController(
    private val reservations: ReserveSeats,
    private val myReservations: ListMyReservations,
    private val payments: PayReservation,
    private val descriptions: DescribeReservation,
) {

    @GetMapping
    fun list(
        @CurrentUser userId: UserId,
        @RequestParam(required = false) currency: String?,
    ): List<ReservationResponse> =
        myReservations.list(userId).map { ReservationResponse.from(descriptions.describe(it, currency)) }

    @PostMapping("/flights/{flightId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun hold(
        @CurrentUser userId: UserId,
        @PathVariable flightId: UUID,
        @RequestParam(required = false) currency: String?,
        @RequestBody @Valid request: HoldSeatsRequest,
    ): ReservationResponse =
        ReservationResponse.from(
            descriptions.describe(
                reservations.hold(
                    userId = userId,
                    flightId = flightId,
                    seats = request.seats.map { it.toRequest() },
                ),
                currency,
            ),
        )

    @PutMapping("/{reservationId}/passengers")
    fun updatePassengers(
        @CurrentUser userId: UserId,
        @PathVariable reservationId: UUID,
        @RequestParam(required = false) currency: String?,
        @RequestBody @Valid request: HoldSeatsRequest,
    ): ReservationResponse =
        ReservationResponse.from(
            descriptions.describe(
                reservations.updatePassengers(
                    userId = userId,
                    reservationId = reservationId,
                    seats = request.seats.map { it.toRequest() },
                ),
                currency,
            ),
        )

    @DeleteMapping("/{reservationId}")
    fun cancel(
        @CurrentUser userId: UserId,
        @PathVariable reservationId: UUID,
        @RequestParam(required = false) currency: String?,
    ): ReservationResponse =
        ReservationResponse.from(descriptions.describe(reservations.cancel(userId, reservationId), currency))

    @DeleteMapping("/{reservationId}/seats")    fun cancelSeats(
        @CurrentUser userId: UserId,
        @PathVariable reservationId: UUID,
        @RequestParam(required = false) currency: String?,
        @RequestBody @Valid request: CancelSeatsRequest,
    ): ReservationResponse =
        ReservationResponse.from(
            descriptions.describe(
                reservations.cancelSeats(userId, reservationId, request.seats.map(SeatNumber::of)),
                currency,
            ),
        )

    @PostMapping("/{reservationId}/payment")
    fun pay(
        @CurrentUser userId: UserId,
        @PathVariable reservationId: UUID,
        @RequestBody @Valid request: PayReservationRequest,
    ): ResponseEntity<PaymentResponse> {
        val payment = payments.pay(userId, reservationId, request.toRequest())
        val body = PaymentResponse.from(payment)
        return if (payment.status == PaymentStatus.APPROVED) {
            ResponseEntity.status(HttpStatus.CREATED).body(body)
        } else {
            ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(body)
        }
    }
}
