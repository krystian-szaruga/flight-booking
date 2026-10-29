package dev.maxaure.flightbooking.mail

import org.springframework.core.io.ClassPathResource
import java.nio.charset.StandardCharsets

internal class MailTemplates(
    private val names: Set<String> = setOf("verification"),
) {

    fun get(name: String): MailTemplate {
        require(name in names) { "Unknown mail template: $name" }
        return MailTemplate(read(name, "subject").trim(), read(name, "txt"), read(name, "html"))
    }

    private fun read(name: String, extension: String): String =
        ClassPathResource("mail/$name.$extension").getContentAsString(StandardCharsets.UTF_8)
}

internal class MailTemplate(val subject: String, val text: String, val html: String)
