package dev.maxaure.flightbooking.bootstrap

import dev.maxaure.flightbooking.application.port.out.MailSender
import dev.maxaure.flightbooking.domain.model.user.Email
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.util.concurrent.ConcurrentHashMap

@TestConfiguration
internal class MailOutboxConfig {

    @Bean
    @Primary
    fun outboxMailSender(): MailSender = MailOutbox
}

internal object MailOutbox : MailSender {
    private val codes = ConcurrentHashMap<String, String>()
    private val sent = ConcurrentHashMap<String, Int>()

    override fun send(template: String, to: Email, model: Map<String, String>) {
        model["code"]?.let {
            codes[to.value] = it
            sent.merge(to.value, 1, Int::plus)
        }
    }

    fun lastCodeFor(email: String): String =
        codes[email] ?: error("No verification code was mailed to $email")

    fun sentCountFor(email: String): Int = sent[email] ?: 0
}
