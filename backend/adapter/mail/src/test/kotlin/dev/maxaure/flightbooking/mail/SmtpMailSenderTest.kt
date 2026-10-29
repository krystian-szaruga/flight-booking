package dev.maxaure.flightbooking.mail

import dev.maxaure.flightbooking.domain.model.user.Email
import jakarta.mail.Multipart
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmtpMailSenderTest {

    @Test
    fun `when a verification mail is sent then subject and both bodies carry the rendered code`() {
        val mail = CapturingMailSender()

        SmtpMailSender(mail, "no-reply@example.com")
            .send("verification", Email.of("anna@example.com"), model("123456"))

        val message = mail.sent.single()
        assertEquals("Your flight-booking verification code: 123456", message.subject)
        assertEquals("anna@example.com", message.getRecipients(MimeMessage.RecipientType.TO).single().toString())
        assertEquals("no-reply@example.com", message.from.single().toString())

        val parts = (message.content as Multipart).let { multipart ->
            (0 until multipart.count).map { (multipart.getBodyPart(it).content as String) }
        }
        assertEquals(2, parts.size)
        parts.forEach { part ->
            assertTrue(part.contains("123456"))
            assertTrue(!part.contains("{appName}") && !part.contains("{code}"))
        }
    }

    @Test
    fun `when the template name is unknown then nothing is mailed`() {
        val mail = CapturingMailSender()

        val failure = runCatching {
            SmtpMailSender(mail, "no-reply@example.com").send("missing", Email.of("anna@example.com"), model("1"))
        }

        assertTrue(failure.isFailure)
        assertTrue(mail.sent.isEmpty())
    }

    private fun model(code: String) =
        mapOf("appName" to "flight-booking", "code" to code, "ttlMinutes" to "15")
}

internal class CapturingMailSender : JavaMailSenderImpl() {
    val sent = mutableListOf<MimeMessage>()

    override fun send(message: MimeMessage) {
        val bytes = ByteArrayOutputStream().also { message.writeTo(it) }.toByteArray()
        sent.add(MimeMessage(null, ByteArrayInputStream(bytes)))
    }
}
