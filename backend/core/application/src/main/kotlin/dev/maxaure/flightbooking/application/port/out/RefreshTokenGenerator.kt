package dev.maxaure.flightbooking.application.port.out

interface RefreshTokenGenerator {
    fun generate(): GeneratedRefreshToken

    fun hash(rawToken: String): String
}

data class GeneratedRefreshToken(
    val raw: String,
    val hash: String,
)
