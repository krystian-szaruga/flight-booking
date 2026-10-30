package dev.maxaure.flightbooking.web.catalogue

import dev.maxaure.flightbooking.application.port.`in`.SearchFlights
import dev.maxaure.flightbooking.domain.NotFoundException
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
    private val flightSearch: SearchFlights,
) {

    @GetMapping
    fun search(
        @RequestParam origin: String,
        @RequestParam destination: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(required = false) cabin: String?,
        @RequestParam(required = false, defaultValue = "Europe/Warsaw") zone: ZoneId,
        @RequestParam(required = false) currency: String?,
    ): List<FlightSummaryResponse> =
        flightSearch.search(origin, destination, from, to ?: from, zone, cabin, currency)
            .map(FlightSummaryResponse::from)

    @GetMapping("/{flightId}/seats")
    fun seatMap(
        @PathVariable flightId: UUID,
        @RequestParam(required = false) currency: String?,
    ): SeatMapResponse =
        flightSearch.seatMap(flightId, currency)
            ?.let(SeatMapResponse::from)
            ?: throw NotFoundException("Flight $flightId is not in the catalogue")

    @GetMapping("/deals")
    fun deals(
        @RequestParam(required = false, defaultValue = "6") limit: Int,
        @RequestParam(required = false) currency: String?,
    ): List<DealResponse> =
        flightSearch.deals(limit, currency).map(DealResponse::from)
}
