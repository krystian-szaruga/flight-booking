package dev.maxaure.flightbooking.application.usecase.user

import dev.maxaure.flightbooking.application.port.`in`.UpdateProfile
import dev.maxaure.flightbooking.application.port.out.UserRepository
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.user.UpdateProfileCommand
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId

class UpdateProfileUseCase(
    private val users: UserRepository,
) : UpdateProfile {

    override fun update(userId: UserId, command: UpdateProfileCommand): User {
        val user = users.findById(userId) ?: throw NotFoundException("User $userId does not exist")
        return users.save(user.rename(command.firstName, command.lastName))
    }
}
