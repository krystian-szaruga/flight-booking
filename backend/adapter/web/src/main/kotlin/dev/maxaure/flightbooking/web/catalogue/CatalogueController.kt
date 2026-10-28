package dev.maxaure.flightbooking.web.catalogue

import dev.maxaure.flightbooking.application.catalogue.search.FlightSearchUseCase
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RestController
@RequestMapping("/api/v1/flights")
internal class CatalogueController(
    private val flightSearch: FlightSearchUseCase,
) {

    @GetMapping
    fun search(
        @RequestParam origin: String,
        @RequestParam destination: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam(required = false) cabin: String?,
        @RequestParam(required = false, defaultValue = "Europe/Warsaw") zone: ZoneId,
    ): List<FlightSummaryResponse> =
        flightSearch.search(origin, destination, date, zone, cabin).map(FlightSummaryResponse::from)

    @GetMapping("/{flightId}/seats")
    fun seatMap(@PathVariable flightId: UUID): SeatMapResponse =
        SeatMapResponse.from(flightSearch.seatMap(flightId))

    @GetMapping("/deals")
    fun deals(
        @RequestParam(required = false, defaultValue = "6") limit: Int,
    ): List<DealResponse> =
        flightSearch.deals(limit).map(DealResponse::from)
}
