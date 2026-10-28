package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.port.out.IngestionRunRepository
import dev.maxaure.flightbooking.domain.model.catalogue.IngestionRun
import dev.maxaure.flightbooking.persistence.mapper.IngestionRunMapper
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

internal class IngestionRunRepositoryAdapter(
    private val jpa: SpringIngestionRunJpaRepository,
    private val transactions: TransactionTemplate,
) : IngestionRunRepository {

    override fun save(run: IngestionRun): IngestionRun = transactions.execute {
        val entity = when (val existing = jpa.findById(run.id).orElse(null)) {
            null -> IngestionRunMapper.toEntity(run)
            else -> existing.apply {
                finishedAt = run.finishedAt
                status = run.status.name
                airportsWritten = run.airportsWritten
                flightsWritten = run.flightsWritten
                rejected = run.rejected
                errorMessage = run.errorMessage
            }
        }
        IngestionRunMapper.toDomain(jpa.save(entity))
    }

    override fun findById(id: UUID): IngestionRun? =
        jpa.findById(id).map(IngestionRunMapper::toDomain).orElse(null)
}
