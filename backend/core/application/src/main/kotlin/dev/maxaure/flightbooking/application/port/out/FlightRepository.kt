package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.catalogue.Flight
import dev.maxaure.flightbooking.domain.model.catalogue.FlightIdentity
import dev.maxaure.flightbooking.domain.model.seat.Seat
import java.time.Instant
import java.util.UUID

interface FlightRepository {

    fun findById(id: UUID): Flight?

    fun findByIdentity(identity: FlightIdentity): Flight?

    fun save(flight: Flight): Flight

    fun replaceSeatInventory(flightId: UUID, seats: List<Seat>)

    fun countSeats(flightId: UUID): Int

    fun countSoldSeats(flightId: UUID): Int

    fun findDeparturesBetween(from: Instant, before: Instant): List<Flight>
}