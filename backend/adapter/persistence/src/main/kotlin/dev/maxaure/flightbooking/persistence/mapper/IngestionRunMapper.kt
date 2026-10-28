package dev.maxaure.flightbooking.persistence.mapper

import dev.maxaure.flightbooking.domain.model.catalogue.IngestionRun
import dev.maxaure.flightbooking.domain.model.catalogue.IngestionStatus
import dev.maxaure.flightbooking.persistence.entity.IngestionRunEntity

internal object IngestionRunMapper {

    fun toDomain(entity: IngestionRunEntity): IngestionRun = IngestionRun(
        id = entity.id,
        source = entity.source,
        startedAt = entity.startedAt,
        finishedAt = entity.finishedAt,
        status = IngestionStatus.valueOf(entity.status),
        airportsWritten = entity.airportsWritten,
        flightsWritten = entity.flightsWritten,
        rejected = entity.rejected,
        errorMessage = entity.errorMessage,
    )

    fun toEntity(run: IngestionRun): IngestionRunEntity = IngestionRunEntity(
        id = run.id,
        source = run.source,
        startedAt = run.startedAt,
        finishedAt = run.finishedAt,
        status = run.status.name,
        airportsWritten = run.airportsWritten,
        flightsWritten = run.flightsWritten,
        rejected = run.rejected,
        errorMessage = run.errorMessage,
    )
}
