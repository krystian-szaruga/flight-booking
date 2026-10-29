package dev.maxaure.flightbooking.mail

import dev.maxaure.flightbooking.application.port.out.MailSender
import dev.maxaure.flightbooking.domain.model.user.Email
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.mail.javamail.JavaMailSender
import java.util.Properties

@Configuration
internal class MailAdapterConfig {

    @Bean
    @Conditional(OnMailEnabled::class)
    fun smtpMailSender(
        javaMailSender: JavaMailSender,
        @Value("\${app.mail.from}") from: String,
    ): MailSender = SmtpMailSender(javaMailSender, from)

    private class OnMailEnabled : Condition {
        override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
            context.environment.getProperty("app.mail.enabled", Boolean::class.java, true)
    }
}
