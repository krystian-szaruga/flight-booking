package dev.maxaure.flightbooking.domain.model.catalogue

import java.util.UUID

data class IngestionSummary(
    val runId: UUID,
    val status: IngestionStatus,
    val airports: Int,
    val flights: Int,
    val rejected: Int,
)
