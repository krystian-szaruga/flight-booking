package dev.maxaure.flightbooking.domain.model.seat

import dev.maxaure.flightbooking.domain.model.codes.SeatNumber
import java.util.UUID

data class Seat(
    val seatNumber: SeatNumber,
    val fareClassId: UUID,
    val status: SeatStatus = SeatStatus.AVAILABLE,
    val version: Long = 0,
)

enum class SeatStatus { AVAILABLE, HELD, SOLD }
