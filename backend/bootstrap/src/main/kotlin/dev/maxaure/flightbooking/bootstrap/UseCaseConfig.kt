package dev.maxaure.flightbooking.bootstrap

import dev.maxaure.flightbooking.application.catalogue.CatalogueIngestionUseCase
import dev.maxaure.flightbooking.application.catalogue.repository.AirportRepository
import dev.maxaure.flightbooking.application.catalogue.repository.FlightRepository
import dev.maxaure.flightbooking.application.catalogue.repository.IngestionRunRepository
import dev.maxaure.flightbooking.application.catalogue.repository.RecordWriter
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
}
