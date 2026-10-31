package dev.maxaure.flightbooking.web.auth

import dev.maxaure.flightbooking.application.port.out.AccessTokenIssuer
import dev.maxaure.flightbooking.application.port.out.RevokedTokenRepository
import jakarta.servlet.Filter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration

@Configuration
internal class AuthWebConfig : WebMvcConfigurer {

    @Bean
    fun authCookie(
        @Value("\${app.auth.jwt-ttl-minutes}") ttlMinutes: Long,
        @Value("\${app.auth.cookie.secure}") secure: Boolean,
        @Value("\${app.auth.cookie.same-site}") sameSite: String,
    ): AuthCookie = AuthCookie(Duration.ofMinutes(ttlMinutes), secure, sameSite)

    @Bean
    fun refreshCookie(
        @Value("\${app.auth.refresh-ttl-days}") ttlDays: Long,
        @Value("\${app.auth.cookie.secure}") secure: Boolean,
        @Value("\${app.auth.cookie.same-site}") sameSite: String,
    ): RefreshCookie = RefreshCookie(Duration.ofDays(ttlDays), secure, sameSite)

    @Bean("authFilter")
    fun authFilter(tokens: AccessTokenIssuer, revokedTokens: RevokedTokenRepository): Filter =
        AuthFilter(tokens, revokedTokens)

    @Bean
    fun authFilterRegistration(@Qualifier("authFilter") authFilter: Filter): FilterRegistrationBean<Filter> =
        FilterRegistrationBean(authFilter).apply { isEnabled = false }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(CurrentUserResolver())
    }
}