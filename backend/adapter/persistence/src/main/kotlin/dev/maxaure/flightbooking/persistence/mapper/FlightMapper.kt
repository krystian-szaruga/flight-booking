package dev.maxaure.flightbooking.persistence.mapper

import dev.maxaure.flightbooking.domain.catalogue.model.Cabin
import dev.maxaure.flightbooking.domain.catalogue.model.FareClass
import dev.maxaure.flightbooking.domain.catalogue.model.Flight
import dev.maxaure.flightbooking.domain.catalogue.model.FlightNumber
import dev.maxaure.flightbooking.domain.catalogue.model.FlightStatus
import dev.maxaure.flightbooking.domain.catalogue.model.Price
import dev.maxaure.flightbooking.persistence.entity.FareClassEntity
import dev.maxaure.flightbooking.persistence.entity.FlightEntity

internal object FlightMapper {

    fun toDomain(entity: FlightEntity): Flight = Flight(
        id = entity.id,
        flightNumber = FlightNumber.fromTrusted(entity.flightNumber),
        origin = AirportMapper.toDomain(entity.origin),
        destination = AirportMapper.toDomain(entity.destination),
        departureAt = entity.departureAt,
        arrivalAt = entity.arrivalAt,
        aircraft = entity.aircraft,
        status = FlightStatus.parse(entity.status),
        fares = entity.fares.map(::fareToDomain),
        source = entity.source,
        syncedAt = entity.syncedAt,
    )

    fun toEntity(flight: Flight): FlightEntity {
        val entity = FlightEntity(
            id = flight.id,
            flightNumber = flight.flightNumber.value,
            origin = AirportMapper.toEntity(flight.origin),
            destination = AirportMapper.toEntity(flight.destination),
            departureAt = flight.departureAt,
            arrivalAt = flight.arrivalAt,
            aircraft = flight.aircraft,
            status = flight.status.name,
            source = flight.source,
            syncedAt = flight.syncedAt,
        )
        flight.fares.forEach { entity.fares.add(fareToEntity(it, entity)) }
        return entity
    }

    private fun fareToDomain(entity: FareClassEntity): FareClass = FareClass(
        id = entity.id,
        cabin = Cabin.parse(entity.cabin),
        price = Price(entity.basePriceAmount, entity.currency),
        multiplier = entity.multiplier,
    )

    private fun fareToEntity(fare: FareClass, flight: FlightEntity): FareClassEntity = FareClassEntity(
        id = fare.id,
        flight = flight,
        cabin = fare.cabin.name,
        basePriceAmount = fare.price.amount,
        currency = fare.price.currency,
        multiplier = fare.multiplier,
    )
}
