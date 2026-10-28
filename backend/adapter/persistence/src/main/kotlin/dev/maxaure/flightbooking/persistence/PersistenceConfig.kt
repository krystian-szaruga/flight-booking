package dev.maxaure.flightbooking.persistence

import dev.maxaure.flightbooking.application.port.out.AirportRepository
import dev.maxaure.flightbooking.application.port.out.FlightRepository
import dev.maxaure.flightbooking.application.port.out.IngestionRunRepository
import dev.maxaure.flightbooking.application.port.out.RecordWriter
import dev.maxaure.flightbooking.application.port.out.AirportDirectory
import dev.maxaure.flightbooking.application.port.out.FlightSearch
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.application.port.out.RevokedTokenRepository
import dev.maxaure.flightbooking.persistence.repository.AirportDirectoryAdapter
import dev.maxaure.flightbooking.persistence.repository.AirportRepositoryAdapter
import dev.maxaure.flightbooking.persistence.repository.FlightRepositoryAdapter
import dev.maxaure.flightbooking.persistence.repository.FlightSearchAdapter
import dev.maxaure.flightbooking.persistence.repository.IngestionRunRepositoryAdapter
import dev.maxaure.flightbooking.persistence.repository.ReservationRepositoryAdapter
import dev.maxaure.flightbooking.persistence.repository.RevokedTokenRepositoryAdapter
import dev.maxaure.flightbooking.persistence.repository.SpringAirportJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringFareClassJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringFlightJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringFlightSearchJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringFlightSeatJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringRevokedTokenJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringIngestionRunJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringReservationJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringReservationSeatJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringSeatMapJpaRepository
import dev.maxaure.flightbooking.persistence.repository.SpringUserJpaRepository
import dev.maxaure.flightbooking.persistence.repository.TransactionalRecordWriter
import dev.maxaure.flightbooking.persistence.repository.UserRepositoryAdapter
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

    @Bean
    fun flightSearch(
        search: SpringFlightSearchJpaRepository,
        seats: SpringSeatMapJpaRepository,
    ): FlightSearch = FlightSearchAdapter(search, seats)

    @Bean
    fun reservationRepository(
        reservations: SpringReservationJpaRepository,
        reservationSeats: SpringReservationSeatJpaRepository,
        transactions: PlatformTransactionManager,
    ): ReservationRepository = ReservationRepositoryAdapter(
        reservations,
        reservationSeats,
        TransactionTemplate(transactions),
    )

    @Bean
    fun airportDirectory(jpa: SpringAirportJpaRepository): AirportDirectory =
        AirportDirectoryAdapter(jpa)

    @Bean
    fun userRepository(jpa: SpringUserJpaRepository): UserRepository =
        UserRepositoryAdapter(jpa)

    @Bean
    fun revokedTokenRepository(
        jpa: SpringRevokedTokenJpaRepository,
        transactions: PlatformTransactionManager,
    ): RevokedTokenRepository = RevokedTokenRepositoryAdapter(
        jpa,
        TransactionTemplate(transactions),
    )
}
