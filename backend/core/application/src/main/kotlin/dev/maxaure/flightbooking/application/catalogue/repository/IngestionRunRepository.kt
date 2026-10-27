package dev.maxaure.flightbooking.application.catalogue.repository

import dev.maxaure.flightbooking.domain.catalogue.model.IngestionRun
import java.util.UUID

interface IngestionRunRepository {
    fun save(run: IngestionRun): IngestionRun

    fun findById(id: UUID): IngestionRun?
}
