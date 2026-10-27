package dev.maxaure.flightbooking.domain.catalogue.model

import java.time.Instant
import java.util.UUID

enum class IngestionStatus { RUNNING, SUCCEEDED, PARTIAL, FAILED }

data class IngestionRun(
    val id: UUID,
    val source: String,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val status: IngestionStatus = IngestionStatus.RUNNING,
    val airportsWritten: Int = 0,
    val flightsWritten: Int = 0,
    val rejected: Int = 0,
    val errorMessage: String? = null,
) {
    init {
        require(source.isNotBlank()) { "Source must not be blank" }
        require(airportsWritten >= 0 && flightsWritten >= 0 && rejected >= 0) { "Counters must not be negative" }
        require(finishedAt == null || !finishedAt.isBefore(startedAt)) { "A run cannot finish before it starts" }
        require((status == IngestionStatus.RUNNING) == (finishedAt == null)) { "Only a running run has no finish time" }
    }

    fun finish(status: IngestionStatus, at: Instant, errorMessage: String? = null): IngestionRun =
        copy(status = status, finishedAt = at, errorMessage = errorMessage)

    companion object {
        fun start(source: String, at: Instant, id: UUID = UUID.randomUUID()): IngestionRun =
            IngestionRun(id = id, source = source, startedAt = at)
    }
}
