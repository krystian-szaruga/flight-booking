package dev.maxaure.flightbooking.application.port.out

data class LoginAttemptKey(val address: String, val email: String)

interface LoginAttemptLimiter {

    fun check(key: LoginAttemptKey)

    fun recordFailure(key: LoginAttemptKey)

    fun clear(key: LoginAttemptKey)
}
