package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand
import dev.maxaure.flightbooking.domain.model.user.verification.VerificationOutcome

interface VerifyEmail {
    fun verify(command: VerifyEmailCommand): VerificationOutcome
}
