package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.user.RevokeTokenCommand

interface RevokeToken {
    fun revoke(command: RevokeTokenCommand)
}
