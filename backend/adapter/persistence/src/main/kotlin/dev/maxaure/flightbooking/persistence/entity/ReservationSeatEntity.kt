package dev.maxaure.flightbooking.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "reservation_seat")
internal class ReservationSeatEntity(
    @Id
    var id: UUID,

    @Column(name = "reservation_id", nullable = false)
    var reservationId: UUID,

    @Column(name = "flight_seat_id", nullable = false)
    var flightSeatId: UUID,

    @Column(name = "first_name", nullable = false)
    var firstName: String,

    @Column(name = "last_name", nullable = false)
    var lastName: String,

    @Column(name = "passport_number_ciphertext")
    var passportNumberCiphertext: String? = null,

    @Column(name = "passport_number_last4")
    var passportNumberLast4: String? = null,

    @Column(name = "phone_number_ciphertext")
    var phoneNumberCiphertext: String? = null,

    @Column(name = "phone_number_last4")
    var phoneNumberLast4: String? = null,

    @Column(name = "date_of_birth")
    var dateOfBirth: LocalDate? = null,
)
