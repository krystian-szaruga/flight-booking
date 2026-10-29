package dev.maxaure.flightbooking.persistence.mapper

import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.verification.PendingRegistration
import dev.maxaure.flightbooking.persistence.entity.PendingRegistrationEntity

internal object PendingRegistrationMapper {

    fun toDomain(entity: PendingRegistrationEntity): PendingRegistration = PendingRegistration(
        email = Email.fromTrusted(entity.email),
        passwordHash = PasswordHash.fromTrusted(entity.passwordHash),
        codeHash = entity.codeHash,
        expiresAt = entity.expiresAt,
        attempts = entity.attempts,
        createdAt = entity.createdAt,
        sentAt = entity.sentAt,
        sendCount = entity.sendCount,
    )

    fun toEntity(pending: PendingRegistration): PendingRegistrationEntity = PendingRegistrationEntity(
        email = pending.email.value,
        passwordHash = pending.passwordHash.value,
        codeHash = pending.codeHash,
        expiresAt = pending.expiresAt,
        attempts = pending.attempts,
        createdAt = pending.createdAt,
        sentAt = pending.sentAt,
        sendCount = pending.sendCount,
    )
}
