package dev.maxaure.flightbooking.web.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

internal data class RegisterUserRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be a valid address")
    @field:Size(max = 254, message = "Email must be at most 254 characters")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    val password: String,
)

internal data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String,
)
