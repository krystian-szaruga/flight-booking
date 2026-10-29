package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.verification.PendingRegistration
import java.time.Instant

interface PendingRegistrationStore {

    fun find(email: Email): PendingRegistration?

    fun save(pending: PendingRegistration): PendingRegistration

    fun delete(email: Email): Boolean

    fun claimAttempt(email: Email, now: Instant): PendingRegistration?

    fun purgeExpired(before: Instant): Int
}
