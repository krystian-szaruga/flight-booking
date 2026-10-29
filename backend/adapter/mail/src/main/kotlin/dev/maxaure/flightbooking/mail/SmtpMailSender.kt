package dev.maxaure.flightbooking.mail

import dev.maxaure.flightbooking.application.port.out.MailSender
import dev.maxaure.flightbooking.domain.model.user.Email
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.springframework.mail.javamail.JavaMailSender

internal class SmtpMailSender(
    private val mailSender: JavaMailSender,
    private val from: String,
    private val templates: MailTemplates = MailTemplates(),
) : MailSender {

    override fun send(template: String, to: Email, model: Map<String, String>) {
        val body = templates.get(template)
        val message = mailSender.createMimeMessage()
        message.setFrom(InternetAddress(from))
        message.setRecipients(MimeMessage.RecipientType.TO, to.value)
        message.subject = render(body.subject, model).trim().replace("\n", " ")
        message.setContent(
            MimeMultipart("alternative").apply {
                addBodyPart(MimeBodyPart().apply { setText(render(body.text, model), "UTF-8") })
                addBodyPart(MimeBodyPart().apply { setText(render(body.html, model), "UTF-8", "html") })
            },
        )
        mailSender.send(message)
    }

    private fun render(body: String, model: Map<String, String>): String =
        model.entries.fold(body) { rendered, (key, value) -> rendered.replace("{$key}", value) }
}
