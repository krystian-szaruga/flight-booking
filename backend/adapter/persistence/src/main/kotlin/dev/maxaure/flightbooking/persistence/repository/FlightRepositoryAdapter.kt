package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.FlightRepository
import dev.maxaure.flightbooking.domain.model.pricing.FareClass
import dev.maxaure.flightbooking.domain.model.catalogue.Flight
import dev.maxaure.flightbooking.domain.model.catalogue.FlightIdentity
import dev.maxaure.flightbooking.domain.model.seat.Seat
import dev.maxaure.flightbooking.persistence.entity.FareClassEntity
import dev.maxaure.flightbooking.persistence.entity.FlightEntity
import dev.maxaure.flightbooking.persistence.entity.FlightSeatEntity
import dev.maxaure.flightbooking.persistence.mapper.AirportMapper
import dev.maxaure.flightbooking.persistence.mapper.FlightMapper
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Transactional(readOnly = true)
internal class FlightRepositoryAdapter(
    private val flights: SpringFlightJpaRepository,
    private val fares: SpringFareClassJpaRepository,
    private val seats: SpringFlightSeatJpaRepository,
) : FlightRepository {

    override fun findById(id: UUID): Flight? =
        flights.findById(id).map(FlightMapper::toDomain).orElse(null)

    override fun findByIdentity(identity: FlightIdentity): Flight? =
        flights.findByFlightNumberAndDepartureAt(identity.flightNumber.value, identity.departureAt)
            ?.let(::toDomainWithLayout)

    @Transactional
    override fun save(flight: Flight): Flight {
        val entity = when (val existing = flights.findById(flight.id).orElse(null)) {
            null -> FlightMapper.toEntity(flight)
            else -> existing.apply {
                flightNumber = flight.flightNumber.value
                origin = AirportMapper.toEntity(flight.origin)
                destination = AirportMapper.toEntity(flight.destination)
                departureAt = flight.departureAt
                arrivalAt = flight.arrivalAt
                aircraft = flight.aircraft
                status = flight.status.name
                source = flight.source
                syncedAt = flight.syncedAt
                syncFares(flight.fares)

            }
        }
        return FlightMapper.toDomain(flights.save(entity))
    }

    @Transactional
    override fun replaceSeatInventory(flightId: UUID, seats: List<Seat>) {
        val flight = flights.getReferenceById(flightId)
        val fareClasses = fares.findAllById(seats.map { it.fareClassId }.distinct()).associateBy { it.id }
        val wanted = seats.associateBy { it.seatNumber.value }
        val existing = this.seats.findByFlightIdOrderBySeatNumber(flightId).associateBy { it.seatNumber }

        val obsolete = existing.values.filter { it.seatNumber !in wanted.keys && it.reservationId == null }
        if (obsolete.isNotEmpty()) {
            this.seats.deleteAll(obsolete)
            this.seats.flush()
        }

        seats.forEach { seat ->
            val fareClass = fareClasses.getValue(seat.fareClassId)
            val current = existing[seat.seatNumber.value]
            if (current == null) {
                this.seats.save(
                    FlightSeatEntity(
                        id = UUID.randomUUID(),
                        flight = flight,
                        fareClass = fareClass,
                        seatNumber = seat.seatNumber.value,
                        status = seat.status.name,
                        version = seat.version,
                    ),
                )
            } else if (current.status == AVAILABLE) {
                current.fareClass = fareClass
            }
        }
    }

    override fun countSeats(flightId: UUID): Int = seats.countByFlightId(flightId).toInt()

    override fun countSoldSeats(flightId: UUID): Int =
        seats.findByFlightIdOrderBySeatNumber(flightId).count { it.reservationId != null }

    override fun findDeparturesBetween(from: Instant, before: Instant): List<Flight> =
        flights.findByDepartureAtGreaterThanEqualAndDepartureAtLessThan(from, before).map(::toDomainWithLayout)

    private fun toDomainWithLayout(entity: FlightEntity): Flight {
        val maxRow = seats.findByFlightIdOrderBySeatNumber(entity.id)
            .maxOfOrNull { it.seatNumber.dropLast(1).toInt() }
        val mapped = FlightMapper.toDomain(entity)
        return if (maxRow == null) mapped else mapped.copy(seatRows = maxRow)
    }

    private companion object {
        const val AVAILABLE = "AVAILABLE"
    }

    private fun FlightEntity.syncFares(incoming: List<FareClass>) {

        val existingById = fares.associateBy { it.id }
        val incomingIds = incoming.map { it.id }.toSet()
        this.fares.removeIf { it.id !in incomingIds }
        incoming.forEach { fare ->
            when (val current = existingById[fare.id]) {
                null -> this.fares.add(
                    FareClassEntity(
                        id = fare.id,
                        flight = this,
                        cabin = fare.cabin.name,
                        basePriceAmount = fare.price.amount,
                        currency = fare.price.currency,
                        multiplier = fare.multiplier,
                    ),
                )
                else -> current.apply {
                    cabin = fare.cabin.name
                    basePriceAmount = fare.price.amount
                    currency = fare.price.currency
                    multiplier = fare.multiplier
                }
            }
        }
    }
}
