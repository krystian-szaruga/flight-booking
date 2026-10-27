package dev.maxaure.flightbooking.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ingestion_run")
internal class IngestionRunEntity(
    @Id
    var id: UUID,

    @Column(name = "source", nullable = false)
    var source: String,

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant,

    @Column(name = "finished_at")
    var finishedAt: Instant? = null,

    @Column(name = "status", nullable = false)
    var status: String = "RUNNING",

    @Column(name = "airports_written", nullable = false)
    var airportsWritten: Int = 0,

    @Column(name = "flights_written", nullable = false)
    var flightsWritten: Int = 0,

    @Column(name = "rejected", nullable = false)
    var rejected: Int = 0,

    @Column(name = "error_message")
    var errorMessage: String? = null,
)
