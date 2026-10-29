package dev.maxaure.flightbooking.web.auth

import java.time.Duration
import org.springframework.http.ResponseCookie

internal class AuthCookie(
    private val ttl: Duration,
    private val secure: Boolean,
    private val sameSite: String,
) {
    fun issue(token: String): ResponseCookie = base(token, ttl).build()

    fun clear(): ResponseCookie = base("", Duration.ZERO).build()

    private fun base(value: String, maxAge: Duration): ResponseCookie.ResponseCookieBuilder =
        ResponseCookie.from(NAME, value)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .sameSite(sameSite)
            .maxAge(maxAge)

    companion object {
        const val NAME = "access_token"
    }
}
