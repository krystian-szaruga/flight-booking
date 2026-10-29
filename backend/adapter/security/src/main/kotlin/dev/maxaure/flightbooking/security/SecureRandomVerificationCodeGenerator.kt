package dev.maxaure.flightbooking.security

import dev.maxaure.flightbooking.application.port.out.VerificationCodeGenerator
import java.security.SecureRandom

internal class SecureRandomVerificationCodeGenerator(
    private val random: SecureRandom = SecureRandom(),
) : VerificationCodeGenerator {

    override fun next(): String = random.nextInt(BOUND).toString().padStart(LENGTH, '0')

    private companion object {
        const val LENGTH = 6
        const val BOUND = 1_000_000
    }
}
