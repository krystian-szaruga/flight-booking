package dev.maxaure.flightbooking.bootstrap

import dev.maxaure.flightbooking.application.catalogue.CatalogueIngestionUseCase
import dev.maxaure.flightbooking.application.catalogue.airport.AirportDirectory
import dev.maxaure.flightbooking.application.catalogue.airport.SearchAirportsUseCase
import dev.maxaure.flightbooking.application.catalogue.repository.AirportRepository
import dev.maxaure.flightbooking.application.catalogue.repository.FlightRepository
import dev.maxaure.flightbooking.application.catalogue.repository.IngestionRunRepository
import dev.maxaure.flightbooking.application.catalogue.repository.RecordWriter
import dev.maxaure.flightbooking.application.catalogue.search.FlightSearch
import dev.maxaure.flightbooking.application.catalogue.search.FlightSearchUseCase
import dev.maxaure.flightbooking.application.reservation.ReservationRepository
import dev.maxaure.flightbooking.application.reservation.ReservationUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
internal class UseCaseConfig {

    @Bean
    fun catalogueIngestionUseCase(
        airports: AirportRepository,
        flights: FlightRepository,
        runs: IngestionRunRepository,
        records: RecordWriter,
    ): CatalogueIngestionUseCase = CatalogueIngestionUseCase(airports, flights, runs, records, Clock.systemUTC())

    @Bean
    fun flightSearchUseCase(search: FlightSearch): FlightSearchUseCase =
        FlightSearchUseCase(search, Clock.systemUTC())

    @Bean
    fun searchAirportsUseCase(directory: AirportDirectory): SearchAirportsUseCase =
        SearchAirportsUseCase(directory)

    @Bean
    fun reservationUseCase(reservations: ReservationRepository): ReservationUseCase =
        ReservationUseCase(reservations, Clock.systemUTC())
}
