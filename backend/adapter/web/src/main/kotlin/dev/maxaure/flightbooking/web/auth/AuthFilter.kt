package dev.maxaure.flightbooking.web.auth

import dev.maxaure.flightbooking.application.port.out.AccessTokenIssuer
import dev.maxaure.flightbooking.application.port.out.RevokedTokenRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

internal class AuthFilter(
    private val tokens: AccessTokenIssuer,
    private val revokedTokens: RevokedTokenRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        if (SecurityContextHolder.getContext().authentication == null) {
            request.cookies
                ?.firstOrNull { it.name == AuthCookie.NAME }
                ?.let { tokens.parse(it.value) }
                ?.takeUnless { revokedTokens.isRevoked(it.id) }
                ?.let { token ->
                    val context = SecurityContextHolder.createEmptyContext()
                    context.authentication =
                        UsernamePasswordAuthenticationToken.authenticated(token.userId, null, emptyList())
                    SecurityContextHolder.setContext(context)
                }
        }
        chain.doFilter(request, response)
    }
}