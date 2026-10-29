package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.domain.model.user.Email
import dev.maxaure.flightbooking.domain.model.user.EmailAlreadyRegistered
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import dev.maxaure.flightbooking.persistence.mapper.UserMapper
import org.springframework.dao.DataIntegrityViolationException

internal class UserRepositoryAdapter(
    private val jpa: SpringUserJpaRepository,
) : UserRepository {

    override fun findById(id: UserId): User? =
        jpa.findById(id.value).orElse(null)?.let(UserMapper::toDomain)

    override fun findByEmail(email: Email): User? =
        jpa.findByEmail(email.value)?.let(UserMapper::toDomain)

    override fun save(user: User): User =
        try {
            UserMapper.toDomain(jpa.save(UserMapper.toEntity(user)))
        } catch (e: DataIntegrityViolationException) {
            throw EmailAlreadyRegistered(user.email)
        }
}
