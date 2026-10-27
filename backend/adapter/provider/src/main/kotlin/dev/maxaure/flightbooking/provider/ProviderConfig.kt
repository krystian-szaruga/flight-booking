package dev.maxaure.flightbooking.provider

import dev.maxaure.flightbooking.application.catalogue.feed.port.FlightDataProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import tools.jackson.databind.ObjectMapper

@Configuration
internal class ProviderConfig {

    @Bean
    @ConditionalOnProperty("catalogue.providers.file.enabled", havingValue = "true")
    fun fileFlightDataProvider(
        objectMapper: ObjectMapper,
        resourceLoader: ResourceLoader,
        @Value($$"${catalogue.providers.file.location}") location: String,
    ): FlightDataProvider = FileFlightDataProvider(objectMapper, resourceLoader, location)
}