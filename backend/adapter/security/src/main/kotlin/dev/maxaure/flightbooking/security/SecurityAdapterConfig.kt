package dev.maxaure.flightbooking.security

import dev.maxaure.flightbooking.application.port.out.AccessTokenIssuer
import dev.maxaure.flightbooking.application.port.out.LoginAttemptLimiter
import dev.maxaure.flightbooking.application.port.out.PasswordHasher
import java.time.Clock
import java.time.Duration
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@Configuration
internal class SecurityAdapterConfig {

    @Bean
    fun passwordHasher(): PasswordHasher = BCryptPasswordHasher(BCryptPasswordEncoder())

    @Bean
    fun accessTokenIssuer(
        @Value("\${app.auth.jwt-secret}") secret: String,
        @Value("\${app.auth.jwt-ttl-minutes}") ttlMinutes: Long,
        clock: Clock,
    ): AccessTokenIssuer = JwtAccessTokenIssuer(secret, Duration.ofMinutes(ttlMinutes), clock)

    @Bean
    fun loginAttemptLimiter(
        @Value("\${app.auth.login-attempts.max-failures}") maxFailures: Int,
        @Value("\${app.auth.login-attempts.window-minutes}") windowMinutes: Long,
        clock: Clock,
    ): LoginAttemptLimiter =
        InMemoryLoginAttemptLimiter(maxFailures, Duration.ofMinutes(windowMinutes), clock)
}
