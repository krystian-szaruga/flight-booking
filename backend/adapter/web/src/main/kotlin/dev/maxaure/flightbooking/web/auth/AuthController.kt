package dev.maxaure.flightbooking.web.auth

import dev.maxaure.flightbooking.application.port.`in`.AuthenticateUser
import dev.maxaure.flightbooking.application.port.`in`.GetCurrentUser
import dev.maxaure.flightbooking.application.port.`in`.RegisterUser
import dev.maxaure.flightbooking.application.port.`in`.RevokeToken
import dev.maxaure.flightbooking.application.port.out.AccessTokenIssuer
import dev.maxaure.flightbooking.domain.model.user.AuthenticateUserCommand
import dev.maxaure.flightbooking.domain.model.user.RegisterUserCommand
import dev.maxaure.flightbooking.domain.model.user.RevokeTokenCommand
import dev.maxaure.flightbooking.domain.model.user.User
import dev.maxaure.flightbooking.domain.model.user.UserId
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
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
    private val authenticateUser: AuthenticateUser,
    private val getCurrentUser: GetCurrentUser,
    private val revokeToken: RevokeToken,
    private val tokens: AccessTokenIssuer,
    private val cookie: AuthCookie,
) {

    @PostMapping("/register")
    fun register(@RequestBody @Valid request: RegisterUserRequest): ResponseEntity<UserResponse> {
        val user = registerUser.register(RegisterUserCommand(request.email, request.password))
        return signedIn(user, HttpStatus.CREATED)
    }

    @PostMapping("/login")
    fun login(
        @RequestBody @Valid request: LoginRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<UserResponse> {
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

    private fun signedIn(user: User, status: HttpStatus): ResponseEntity<UserResponse> =
        ResponseEntity.status(status)
            .header(HttpHeaders.SET_COOKIE, cookie.issue(tokens.issue(user.id)).toString())
            .body(UserResponse.from(user))
}
