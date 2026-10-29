package dev.maxaure.flightbooking.application.port.out

interface VerificationCodeGenerator {
    fun next(): String
}
