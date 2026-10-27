package dev.maxaure.flightbooking.application.catalogue.repository

import dev.maxaure.flightbooking.domain.catalogue.model.Flight
import dev.maxaure.flightbooking.domain.catalogue.model.FlightIdentity
import dev.maxaure.flightbooking.domain.catalogue.model.Seat
import java.time.Instant
import java.util.UUID

interface FlightRepository {

    fun findById(id: UUID): Flight?

    fun findByIdentity(identity: FlightIdentity): Flight?

    fun save(flight: Flight): Flight

    fun replaceSeatInventory(flightId: UUID, seats: List<Seat>)

    fun countSeats(flightId: UUID): Int

    fun findDeparturesBetween(from: Instant, before: Instant): List<Flight>
}