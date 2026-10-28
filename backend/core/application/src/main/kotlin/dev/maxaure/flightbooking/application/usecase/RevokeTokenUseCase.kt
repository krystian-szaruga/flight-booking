package dev.maxaure.flightbooking.application.usecase

import dev.maxaure.flightbooking.application.port.`in`.RevokeToken
import dev.maxaure.flightbooking.application.port.out.RevokedTokenRepository
import dev.maxaure.flightbooking.domain.model.user.RevokeTokenCommand

class RevokeTokenUseCase(
    private val revoked: RevokedTokenRepository,
) : RevokeToken {

    override fun revoke(command: RevokeTokenCommand) {
        revoked.revoke(command.tokenId, command.expiresAt)
    }
}
