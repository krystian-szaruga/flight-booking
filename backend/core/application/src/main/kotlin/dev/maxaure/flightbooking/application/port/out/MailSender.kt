package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.user.Email

interface MailSender {
    fun send(template: String, to: Email, model: Map<String, String>)
}
