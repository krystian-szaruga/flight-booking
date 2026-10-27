package dev.maxaure.flightbooking.domain.catalogue.model

import java.util.UUID

data class IngestionOutcome(
    val runId: UUID,
    val status: IngestionStatus,
    val airports: Int,
    val flights: Int,
    val rejected: Int,
)
