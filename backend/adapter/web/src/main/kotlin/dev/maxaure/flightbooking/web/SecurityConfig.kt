package dev.maxaure.flightbooking.web

import jakarta.servlet.Filter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter


@Configuration
internal class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        @Qualifier("authFilter") authFilter: Filter,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/actuator/health").permitAll()
                it.requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/logout",
                    "/api/auth/verify",
                    "/api/auth/verify/resend",
                ).permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/flights/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/airports").permitAll()
                it.anyRequest().authenticated()
            }
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}