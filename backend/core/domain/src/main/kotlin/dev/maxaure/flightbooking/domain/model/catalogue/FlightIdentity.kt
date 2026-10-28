package dev.maxaure.flightbooking.domain.model.catalogue

import dev.maxaure.flightbooking.domain.model.codes.FlightNumber
import java.time.Instant

data class FlightIdentity(val flightNumber: FlightNumber, val departureAt: Instant)
