package dev.maxaure.flightbooking.application.usecase.user

import dev.maxaure.flightbooking.application.port.`in`.GetCurrentUser
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId

class GetCurrentUserUseCase(
    private val users: UserRepository,
) : GetCurrentUser {

    override fun currentUser(userId: UserId): User =
        users.findById(userId) ?: throw NotFoundException("User $userId does not exist")
}