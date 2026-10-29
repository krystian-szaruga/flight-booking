package dev.maxaure.flightbooking.web.auth

import dev.maxaure.flightbooking.application.port.`in`.AuthenticateUser
import dev.maxaure.flightbooking.application.port.`in`.GetCurrentUser
import dev.maxaure.flightbooking.application.port.`in`.RegisterUser
import dev.maxaure.flightbooking.application.port.`in`.ResendVerification
import dev.maxaure.flightbooking.application.port.`in`.RevokeToken
import dev.maxaure.flightbooking.application.port.`in`.VerifyEmail
import dev.maxaure.flightbooking.application.port.out.AccessTokenIssuer
import dev.maxaure.flightbooking.domain.model.user.AuthenticateUserCommand
import dev.maxaure.flightbooking.domain.model.user.RegisterUserCommand
import dev.maxaure.flightbooking.domain.model.user.ResendVerificationCommand
import dev.maxaure.flightbooking.domain.model.user.RevokeTokenCommand
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import dev.maxaure.flightbooking.domain.model.user.VerifyEmailCommand
import dev.maxaure.flightbooking.domain.model.user.verification.RegistrationStage
import dev.maxaure.flightbooking.domain.model.user.verification.ResendOutcome
import dev.maxaure.flightbooking.domain.model.user.verification.VerificationOutcome
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
internal class AuthController(
    private val registerUser: RegisterUser,
    private val verifyEmail: VerifyEmail,
    private val resendVerification: ResendVerification,
    private val authenticateUser: AuthenticateUser,
    private val getCurrentUser: GetCurrentUser,
    private val revokeToken: RevokeToken,
    private val tokens: AccessTokenIssuer,
    private val cookie: AuthCookie,
) {

    @PostMapping("/register")
    fun register(@RequestBody @Valid request: RegisterUserRequest): ResponseEntity<RegistrationResponse> {
        val started = registerUser.register(RegisterUserCommand(request.email, request.password))
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(RegistrationResponse(started.stage.name, started.retryAfterSeconds))
    }

    @PostMapping("/verify")
    fun verify(
        @RequestBody @Valid request: VerifyEmailRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<*> {
        val outcome = verifyEmail.verify(
            VerifyEmailCommand(request.email, request.code, httpRequest.remoteAddr),
        )
        return when (outcome) {
            is VerificationOutcome.Verified -> signedIn(outcome.user, HttpStatus.OK)
            VerificationOutcome.AlreadyRegistered -> accountExists()
        }
    }

    @PostMapping("/verify/resend")
    fun resend(
        @RequestBody @Valid request: ResendVerificationRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<*> {
        val outcome = resendVerification.resend(
            ResendVerificationCommand(request.email, httpRequest.remoteAddr),
        )
        return when (outcome) {
            is ResendOutcome.Sent -> ResponseEntity.status(HttpStatus.ACCEPTED)
                .body<ResendResponse>(ResendResponse(outcome.retryAfterSeconds))

            is ResendOutcome.CoolingDown -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, outcome.retryAfterSeconds.coerceAtLeast(1).toString())
                .body<ResendResponse>(ResendResponse(outcome.retryAfterSeconds))

            ResendOutcome.Ignored -> ResponseEntity.noContent().build<Void>()
        }
    }

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: LoginRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Any> {
        val user = authenticateUser.authenticate(
            AuthenticateUserCommand(request.email, request.password, httpRequest.remoteAddr),
        )
        return signedIn(user, HttpStatus.OK)
    }

    @PostMapping("/logout")
    fun logout(@CookieValue(AuthCookie.NAME, required = false) token: String?): ResponseEntity<Void> {
        token?.let { raw -> tokens.parse(raw) }
            ?.let { revokeToken.revoke(RevokeTokenCommand(it.id, it.expiresAt)) }
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cookie.clear().toString())
            .build()
    }

    @GetMapping("/me")
    fun me(@CurrentUser userId: UserId): UserResponse =
        UserResponse.from(getCurrentUser.currentUser(userId))

    private fun signedIn(user: User, status: HttpStatus): ResponseEntity<Any> =
        ResponseEntity.status(status)
            .header(HttpHeaders.SET_COOKIE, cookie.issue(tokens.issue(user.id)).toString())
            .body(UserResponse.from(user))

    private fun accountExists(): ResponseEntity<Any> {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, REGISTERED_ACCOUNT)
        problem.setProperty("stage", RegistrationStage.EMAIL_VERIFIED.name)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem)
    }

    private companion object {
        const val REGISTERED_ACCOUNT = "This email already has an account; sign in instead"
    }
}
