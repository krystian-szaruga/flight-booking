package dev.maxaure.flightbooking.web

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(Info().title("Flight Booking API").version("v1"))
        .components(
            Components().addSecuritySchemes(
                "cookieAuth",
                SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .`in`(SecurityScheme.In.COOKIE)
                    .name("access_token"),
            ),
        )
        .addSecurityItem(SecurityRequirement().addList("cookieAuth"))
}
