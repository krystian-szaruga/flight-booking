package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.user.ResendVerificationCommand
import dev.maxaure.flightbooking.domain.model.user.verification.ResendOutcome

interface ResendVerification {
    fun resend(command: ResendVerificationCommand): ResendOutcome
}
