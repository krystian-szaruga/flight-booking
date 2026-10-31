package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.user.UserId

interface RefreshSession {
    fun start(userId: UserId): RefreshedSession

    fun refresh(rawToken: String): RefreshedSession?

    fun revoke(rawToken: String)
}

data class RefreshedSession(
    val userId: UserId,
    val rawToken: String,
)
