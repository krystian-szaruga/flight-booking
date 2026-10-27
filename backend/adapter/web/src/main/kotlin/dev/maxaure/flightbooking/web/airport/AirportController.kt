package dev.maxaure.flightbooking.web.airport

import dev.maxaure.flightbooking.application.catalogue.airport.SearchAirportsUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/airports")
internal class AirportController(
    private val searchAirports: SearchAirportsUseCase,
) {

    @GetMapping
    fun search(@RequestParam q: String): List<AirportSummaryResponse> =
        searchAirports.search(q).map(AirportSummaryResponse::from)
}
