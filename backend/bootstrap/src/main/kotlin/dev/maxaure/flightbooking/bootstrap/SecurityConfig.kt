package dev.maxaure.flightbooking.bootstrap

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
internal class SecurityConfig {

    @Bean
    fun jwtDecoder(
        @Value($$"${spring.security.oauth2.resourceserver.jwt.issuer-uri}") issuer: String,
        @Value($$"${app.security.feed.audience}") feedAudience: String,
    ): JwtDecoder {
        val decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build()
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(issuer),
                AudienceValidator(feedAudience),
            ),
        )
        return decoder
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/health").permitAll()
                it.anyRequest().denyAll()
            }
            .oauth2ResourceServer { it.jwt(Customizer.withDefaults()) }
        return http.build()
    }

    companion object {
        const val FEED_INGEST_SCOPE = "SCOPE_feed:ingest"
    }
}

private class AudienceValidator(private val audience: String) : OAuth2TokenValidator<Jwt> {

    override fun validate(token: Jwt): OAuth2TokenValidatorResult =
        if (token.audience.orEmpty().contains(audience)) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(
                OAuth2Error(
                    "invalid_token",
                    "The required audience '$audience' is missing",
                    null,
                ),
            )
        }
}
