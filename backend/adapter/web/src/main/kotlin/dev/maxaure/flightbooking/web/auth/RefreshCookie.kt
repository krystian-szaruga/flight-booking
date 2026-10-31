package dev.maxaure.flightbooking.web.auth

import org.springframework.http.ResponseCookie
import java.time.Duration

internal class RefreshCookie(
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
            .path(PATH)
            .sameSite(sameSite)
            .maxAge(maxAge)

    companion object {
        const val NAME = "refresh_token"
        const val PATH = "/api/auth"
    }
}
