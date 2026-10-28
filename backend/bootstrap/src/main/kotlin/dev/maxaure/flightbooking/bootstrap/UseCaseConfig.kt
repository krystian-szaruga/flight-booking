package dev.maxaure.flightbooking.bootstrap

import dev.maxaure.flightbooking.application.port.`in`.IngestCatalogue
import dev.maxaure.flightbooking.application.port.`in`.ReserveSeats
import dev.maxaure.flightbooking.application.port.`in`.SearchAirports
import dev.maxaure.flightbooking.application.port.`in`.SearchFlights
import dev.maxaure.flightbooking.application.port.out.AirportDirectory
import dev.maxaure.flightbooking.application.port.out.AirportRepository
import dev.maxaure.flightbooking.application.port.out.FlightRepository
import dev.maxaure.flightbooking.application.port.out.FlightSearch
import dev.maxaure.flightbooking.application.port.out.IngestionRunRepository
import dev.maxaure.flightbooking.application.port.out.RecordWriter
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.application.usecase.CatalogueIngestionUseCase
import dev.maxaure.flightbooking.application.usecase.FlightSearchUseCase
import dev.maxaure.flightbooking.application.usecase.ReservationUseCase
import dev.maxaure.flightbooking.application.usecase.SearchAirportsUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
internal class UseCaseConfig {

    @Bean
    fun ingestCatalogue(
        airports: AirportRepository,
        flights: FlightRepository,
        runs: IngestionRunRepository,
        records: RecordWriter,
    ): IngestCatalogue = CatalogueIngestionUseCase(airports, flights, runs, records, Clock.systemUTC())

    @Bean
    fun searchFlights(search: FlightSearch): SearchFlights =
        FlightSearchUseCase(search, Clock.systemUTC())

    @Bean
    fun searchAirports(directory: AirportDirectory): SearchAirports =
        SearchAirportsUseCase(directory)

    @Bean
    fun reserveSeats(reservations: ReservationRepository): ReserveSeats =
        ReservationUseCase(reservations, Clock.systemUTC())
}
