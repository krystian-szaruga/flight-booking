package dev.maxaure.flightbooking.provider

import dev.maxaure.flightbooking.application.port.out.FeedRequest
import dev.maxaure.flightbooking.application.port.out.FlightDataProvider
import dev.maxaure.flightbooking.domain.model.catalogue.FeedPage
import org.springframework.core.io.ResourceLoader
import tools.jackson.databind.ObjectMapper
import java.time.ZoneOffset

internal class FileFlightDataProvider(
    private val objectMapper: ObjectMapper,
    private val resourceLoader: ResourceLoader,
    private val location: String,
) : FlightDataProvider {

    override val id = "file"

    private val page: FeedPage by lazy {
        resourceLoader.getResource(location).inputStream.use {
            FeedDocumentMapper.toDomain(objectMapper.readValue(it, FeedDocumentDto::class.java))
        }
    }

    override fun fetch(query: FeedRequest): FeedPage {
        val flights = page.flights.filter { flight ->
            val day = flight.departureAt.atZone(ZoneOffset.UTC).toLocalDate()
            !day.isBefore(query.from) && !day.isAfter(query.to)
        }
        return page.copy(flights = flights)
    }
}