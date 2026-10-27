package dev.maxaure.flightbooking.persistence

import dev.maxaure.flightbooking.application.catalogue.repository.AirportRepository
import dev.maxaure.flightbooking.application.catalogue.repository.FlightRepository
import dev.maxaure.flightbooking.application.catalogue.repository.IngestionRunRepository
import dev.maxaure.flightbooking.application.catalogue.repository.RecordWriter
import dev.maxaure.flightbooking.persistence.repository.AirportRepositoryAdapter
import dev.maxaure.flightbooking.persistence.repository.FlightRepositoryAdapter
import dev.maxaure.flightbooking.persistence.repository.IngestionRunRepositoryAdapter
import dev.maxaure.flightbooking.persistence.repository.SpringAirportJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringFareClassJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringFlightJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringFlightSeatJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringIngestionRunJpaRepository
import dev.maxaure.flightbooking.persistence.repository.TransactionalRecordWriter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Configuration
@EnableJpaRepositories(basePackages = ["dev.maxaure.flightbooking.persistence.repository"])
@ConditionalOnProperty(name = ["app.persistence.enabled"], havingValue = "true", matchIfMissing = true)
internal class PersistenceConfig {

    @Bean
    fun airportRepository(jpa: SpringAirportJpaRepository): AirportRepository =
        AirportRepositoryAdapter(jpa)

    @Bean
    fun flightRepository(
        flights: SpringFlightJpaRepository,
        fares: SpringFareClassJpaRepository,
        seats: SpringFlightSeatJpaRepository,
    ): FlightRepository = FlightRepositoryAdapter(flights, fares, seats)

    @Bean
    fun ingestionRunRepository(
        jpa: SpringIngestionRunJpaRepository,
        transactions: PlatformTransactionManager,
    ): IngestionRunRepository = IngestionRunRepositoryAdapter(
        jpa,
        TransactionTemplate(transactions).apply { propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW },
    )

    @Bean
    fun recordWriter(transactions: PlatformTransactionManager): RecordWriter =
        TransactionalRecordWriter(TransactionTemplate(transactions))
}
