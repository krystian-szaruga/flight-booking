package dev.maxaure.flightbooking.bootstrap

import dev.maxaure.flightbooking.application.port.`in`.IngestCatalogue
import dev.maxaure.flightbooking.application.port.`in`.AuthenticateUser
import dev.maxaure.flightbooking.application.port.`in`.DescribeReservation
import dev.maxaure.flightbooking.application.port.`in`.GetCurrentUser
import dev.maxaure.flightbooking.application.port.`in`.ListCurrencies
import dev.maxaure.flightbooking.application.port.`in`.ListMyReservations
import dev.maxaure.flightbooking.application.port.`in`.PayReservation
import dev.maxaure.flightbooking.application.port.`in`.RegisterUser
import dev.maxaure.flightbooking.application.port.`in`.ResendVerification
import dev.maxaure.flightbooking.application.port.`in`.ReserveSeats
import dev.maxaure.flightbooking.application.port.`in`.RevokeToken
import dev.maxaure.flightbooking.application.port.`in`.SearchAirports
import dev.maxaure.flightbooking.application.port.`in`.SearchFlights
import dev.maxaure.flightbooking.application.port.`in`.UpdateProfile
import dev.maxaure.flightbooking.application.port.`in`.VerifyEmail
import dev.maxaure.flightbooking.application.port.out.MailSender
import dev.maxaure.flightbooking.application.port.out.PendingRegistrationStore
import dev.maxaure.flightbooking.application.port.out.VerificationCodeGenerator
import dev.maxaure.flightbooking.application.port.out.AirportDirectory
import dev.maxaure.flightbooking.application.port.out.AirportRepository
import dev.maxaure.flightbooking.application.port.out.CurrencyConverter
import dev.maxaure.flightbooking.application.port.out.FlightRepository
import dev.maxaure.flightbooking.application.port.out.FlightSearch
import dev.maxaure.flightbooking.application.port.out.IngestionRunRepository
import dev.maxaure.flightbooking.application.port.out.LoginAttemptLimiter
import dev.maxaure.flightbooking.application.port.out.PaymentGateway
import dev.maxaure.flightbooking.application.port.out.PaymentRepository
import dev.maxaure.flightbooking.application.port.out.RecordWriter
import dev.maxaure.flightbooking.application.port.out.ReservationPricer
import dev.maxaure.flightbooking.application.port.out.ReservationRepository
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.application.port.out.RevokedTokenRepository
import dev.maxaure.flightbooking.application.port.out.PasswordHasher
import dev.maxaure.flightbooking.application.usecase.CatalogueIngestionUseCase
import dev.maxaure.flightbooking.application.usecase.DescribeReservationUseCase
import dev.maxaure.flightbooking.application.usecase.FlightSearchUseCase
import dev.maxaure.flightbooking.application.usecase.user.RegisterUserUseCase
import dev.maxaure.flightbooking.application.usecase.user.ResendVerificationUseCase
import dev.maxaure.flightbooking.application.usecase.user.VerifyEmailUseCase
import dev.maxaure.flightbooking.domain.model.user.verification.ResendBackoff
import dev.maxaure.flightbooking.application.usecase.user.AuthenticateUserUseCase
import dev.maxaure.flightbooking.application.usecase.user.GetCurrentUserUseCase
import dev.maxaure.flightbooking.application.usecase.user.UpdateProfileUseCase
import dev.maxaure.flightbooking.application.usecase.ListMyReservationsUseCase
import dev.maxaure.flightbooking.application.usecase.ListCurrenciesUseCase
import dev.maxaure.flightbooking.application.usecase.PayReservationUseCase
import dev.maxaure.flightbooking.application.usecase.RevokeTokenUseCase
import dev.maxaure.flightbooking.application.usecase.ReservationUseCase
import dev.maxaure.flightbooking.application.usecase.SearchAirportsUseCase
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration

@Configuration
internal class UseCaseConfig {

    @Bean
    fun ingestCatalogue(
        airports: AirportRepository,
        flights: FlightRepository,
        runs: IngestionRunRepository,
        records: RecordWriter,
        clock: Clock,
    ): IngestCatalogue = CatalogueIngestionUseCase(airports, flights, runs, records, clock)

    @Bean
    fun searchFlights(search: FlightSearch, converter: CurrencyConverter, clock: Clock): SearchFlights =
        FlightSearchUseCase(search, converter, clock)

    @Bean
    fun searchAirports(directory: AirportDirectory): SearchAirports =
        SearchAirportsUseCase(directory)

    @Bean
    fun reserveSeats(reservations: ReservationRepository, users: UserRepository, clock: Clock): ReserveSeats =
        ReservationUseCase(reservations, users, clock)

    @Bean
    fun registerUser(
        users: UserRepository,
        pending: PendingRegistrationStore,
        hasher: PasswordHasher,
        clock: Clock,
        codes: VerificationCodeGenerator,
        mail: MailSender,
        @Value("\${app.auth.verification.code-ttl-minutes}") ttlMinutes: Long,
        @Value("\${app.mail.app-name}") appName: String,
    ): RegisterUser =
        RegisterUserUseCase(
            users, pending, hasher, clock, codes, mail, Duration.ofMinutes(ttlMinutes), appName,
            ResendBackoff(Duration.ofMinutes(ttlMinutes)),
        )

    @Bean
    fun verifyEmail(
        users: UserRepository,
        pending: PendingRegistrationStore,
        hasher: PasswordHasher,
        loginAttempts: LoginAttemptLimiter,
        clock: Clock,
    ): VerifyEmail = VerifyEmailUseCase(users, pending, hasher, loginAttempts, clock)

    @Bean
    fun resendVerification(
        users: UserRepository,
        pending: PendingRegistrationStore,
        hasher: PasswordHasher,
        loginAttempts: LoginAttemptLimiter,
        codes: VerificationCodeGenerator,
        mail: MailSender,
        clock: Clock,
        @Value("\${app.auth.verification.code-ttl-minutes}") ttlMinutes: Long,
        @Value("\${app.mail.app-name}") appName: String,
    ): ResendVerification =
        ResendVerificationUseCase(
            users, pending, hasher, loginAttempts, codes, mail, clock, Duration.ofMinutes(ttlMinutes), appName,
            ResendBackoff(Duration.ofMinutes(ttlMinutes)),
        )

    @Bean
    fun authenticateUser(
        users: UserRepository,
        hasher: PasswordHasher,
        loginAttempts: LoginAttemptLimiter,
    ): AuthenticateUser =
        AuthenticateUserUseCase(users, hasher, loginAttempts)

    @Bean
    fun getCurrentUser(users: UserRepository): GetCurrentUser = GetCurrentUserUseCase(users)

    @Bean
    fun updateProfile(users: UserRepository): UpdateProfile = UpdateProfileUseCase(users)

    @Bean
    fun revokeToken(revokedTokens: RevokedTokenRepository): RevokeToken =
        RevokeTokenUseCase(revokedTokens)

    @Bean
    fun listMyReservations(reservations: ReservationRepository): ListMyReservations =
        ListMyReservationsUseCase(reservations)

    @Bean
    fun payReservation(
        reservations: ReservationRepository,
        payments: PaymentRepository,
        pricer: ReservationPricer,
        gateway: PaymentGateway,
        converter: CurrencyConverter,
        clock: Clock,
    ): PayReservation = PayReservationUseCase(reservations, payments, pricer, gateway, converter, clock)

    @Bean
    fun describeReservation(
        pricer: ReservationPricer,
        payments: PaymentRepository,
        converter: CurrencyConverter,
    ): DescribeReservation = DescribeReservationUseCase(pricer, payments, converter)

    @Bean
    fun listCurrencies(converter: CurrencyConverter): ListCurrencies = ListCurrenciesUseCase(converter)
}
