package dev.maxaure.flightbooking.application.port.`in`

import dev.maxaure.flightbooking.domain.model.catalogue.FeedPage
import dev.maxaure.flightbooking.domain.model.catalogue.IngestionSummary

interface IngestCatalogue {

    fun ingest(source: String, page: FeedPage): IngestionSummary
}
