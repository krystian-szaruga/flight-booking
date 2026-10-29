package dev.maxaure.flightbooking.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SecureRandomVerificationCodeGeneratorTest {

    @Test
    fun `when many codes are generated then each is six digits including leading zeros`() {
        val generator = SecureRandomVerificationCodeGenerator()

        val codes = (0 until 500).map { generator.next() }

        assertTrue(codes.all { it.length == 6 && it.all(Char::isDigit) })
    }

    @Test
    fun `when the value needs padding then the code keeps leading zeros`() {
        val generator = SecureRandomVerificationCodeGenerator(FixedSecureRandom(42))

        assertEquals("000042", generator.next())
    }

    private class FixedSecureRandom(private val value: Int) : java.security.SecureRandom() {
        override fun nextInt(bound: Int): Int = value
    }
}
