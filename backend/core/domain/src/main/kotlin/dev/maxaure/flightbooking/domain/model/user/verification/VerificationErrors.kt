package dev.maxaure.flightbooking.domain.model.user.verification

import dev.maxaure.flightbooking.domain.DomainException

class InvalidVerificationCode : DomainException("The verification code is invalid or expired.")

class VerificationCodeLocked : DomainException("Too many failed verification attempts; request a new code")
