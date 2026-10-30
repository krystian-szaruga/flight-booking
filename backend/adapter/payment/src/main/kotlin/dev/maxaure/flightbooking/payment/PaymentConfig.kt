package dev.maxaure.flightbooking.payment

import dev.maxaure.flightbooking.application.port.out.PaymentGateway
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class PaymentConfig {

    @Bean
    @ConditionalOnProperty(name = ["app.payments.provider"], havingValue = "fake", matchIfMissing = true)
    fun paymentGateway(): PaymentGateway = FakePaymentGateway()
}
