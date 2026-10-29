package dev.maxaure.flightbooking.bootstrap

import dev.maxaure.flightbooking.domain.ConflictException
import dev.maxaure.flightbooking.domain.InvalidDomainValueException
import dev.maxaure.flightbooking.domain.NotFoundException
import dev.maxaure.flightbooking.domain.model.user.EmailNotVerified
import dev.maxaure.flightbooking.domain.model.user.InvalidCredentials
import dev.maxaure.flightbooking.domain.model.user.ProfileIncomplete
import dev.maxaure.flightbooking.domain.model.user.TooManyLoginAttempts
import dev.maxaure.flightbooking.domain.model.user.verification.InvalidVerificationCode
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
internal class ExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun notFound(ex: NotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Not found")

    @ExceptionHandler(InvalidDomainValueException::class)
    fun invalidValue(ex: InvalidDomainValueException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid value")

    @ExceptionHandler(IllegalArgumentException::class)
    fun illegalArgument(ex: IllegalArgumentException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid value")

    @ExceptionHandler(ConflictException::class)
    fun conflict(ex: ConflictException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.message ?: "Conflict")

    @ExceptionHandler(InvalidCredentials::class)
    fun invalidCredentials(): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials")

    @ExceptionHandler(InvalidVerificationCode::class)
    fun invalidVerificationCode(): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, INVALID_VERIFICATION_CODE)

    @ExceptionHandler(EmailNotVerified::class)
    fun emailNotVerified(): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Verify your email address first")

    @ExceptionHandler(ProfileIncomplete::class)
    fun profileIncomplete(): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Add your first and last name to your profile first")
            .apply { setProperty("code", PROFILE_INCOMPLETE) }

    @ExceptionHandler(TooManyLoginAttempts::class)
    fun tooManyAttempts(ex: TooManyLoginAttempts): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, ex.retryAfter.seconds.coerceAtLeast(1).toString())
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.message ?: "Too many attempts"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalidBody(ex: MethodArgumentNotValidException): ProblemDetail =
        ProblemDetail.forStatus(HttpStatus.BAD_REQUEST).apply {
            title = "Validation failed"
            setProperty("errors", ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage })
        }

    private companion object {
        const val INVALID_VERIFICATION_CODE = "invalid_or_expired_code"
        const val PROFILE_INCOMPLETE = "profile_incomplete"
    }
}
