package dev.maxaure.flightbooking.application.port.out

import dev.maxaure.flightbooking.domain.model.catalogue.IngestionRun
import java.util.UUID

interface IngestionRunRepository {
    fun save(run: IngestionRun): IngestionRun

    fun findById(id: UUID): IngestionRun?
}
