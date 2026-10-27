package dev.maxaure.flightbooking.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "flight_seat")
internal class FlightSeatEntity(
    @Id
    var id: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    var flight: FlightEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fare_class_id", nullable = false)
    var fareClass: FareClassEntity,

    @Column(name = "seat_number")
    var seatNumber: String,

    @Column(name = "status")
    var status: String,

    @Version
    @Column(name = "version")
    var version: Long = 0,

    @Column(name = "hold_expires_at")
    var holdExpiresAt: Instant? = null,

    @Column(name = "reservation_id")
    var reservationId: UUID? = null,
)
