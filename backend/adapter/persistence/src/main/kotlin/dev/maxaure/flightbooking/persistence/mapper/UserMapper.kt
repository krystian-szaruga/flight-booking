package dev.maxaure.flightbooking.persistence.mapper

import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.PasswordHash
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import dev.maxaure.flightbooking.persistence.entity.UserEntity

internal object UserMapper {

    fun toDomain(entity: UserEntity): User = User(
        id = UserId.fromTrusted(entity.id),
        email = Email.fromTrusted(entity.email),
        passwordHash = PasswordHash.fromTrusted(entity.passwordHash),
        createdAt = entity.createdAt,
        emailVerifiedAt = entity.emailVerifiedAt,
    )

    fun toEntity(user: User): UserEntity = UserEntity(
        id = user.id.value,
        email = user.email.value,
        passwordHash = user.passwordHash.value,
        createdAt = user.createdAt,
        emailVerifiedAt = user.emailVerifiedAt,
    )
}
