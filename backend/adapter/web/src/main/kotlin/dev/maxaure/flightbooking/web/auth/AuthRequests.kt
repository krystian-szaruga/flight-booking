package dev.maxaure.flightbooking.web.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
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

internal data class VerifyEmailRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be a valid address")
    @field:Size(max = 254, message = "Email must be at most 254 characters")
    val email: String,

    @field:NotBlank(message = "Code is required")
    @field:Pattern(regexp = "\\d{6}", message = "Code must be 6 digits")
    val code: String,
)

internal data class ResendVerificationRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be a valid address")
    @field:Size(max = 254, message = "Email must be at most 254 characters")
    val email: String,
)

internal data class LoginRequest(    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String,
)

internal data class UpdateProfileRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(max = 100, message = "First name must be at most 100 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(max = 100, message = "Last name must be at most 100 characters")
    val lastName: String,
)
