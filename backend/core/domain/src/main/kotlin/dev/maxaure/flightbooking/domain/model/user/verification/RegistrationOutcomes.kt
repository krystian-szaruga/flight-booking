package dev.maxaure.flightbooking.domain.model.user.verification

import dev.maxaure.flightbooking.domain.model.user.User

enum class RegistrationStage {
    CODE_SENT,
    EMAIL_VERIFIED,
}

data class RegistrationStarted(val stage: RegistrationStage, val retryAfterSeconds: Long)

sealed interface VerificationOutcome {
    data class Verified(val user: User) : VerificationOutcome

    data object AlreadyRegistered : VerificationOutcome
}

sealed interface ResendOutcome {
    data class Sent(val retryAfterSeconds: Long) : ResendOutcome

    data class CoolingDown(val retryAfterSeconds: Long) : ResendOutcome

    data object Ignored : ResendOutcome
}
