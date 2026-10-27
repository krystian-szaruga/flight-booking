package dev.maxaure.flightbooking.scheduler

import dev.maxaure.flightbooking.application.catalogue.CatalogueIngestionUseCase
import dev.maxaure.flightbooking.application.catalogue.feed.port.FeedRequest
import dev.maxaure.flightbooking.application.catalogue.feed.port.FlightDataProvider
import dev.maxaure.flightbooking.application.catalogue.repository.IngestionRunRepository
import dev.maxaure.flightbooking.domain.catalogue.model.IngestionRun
import dev.maxaure.flightbooking.domain.catalogue.model.IngestionStatus
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate

internal class FeedPullJob(
    private val providers: List<FlightDataProvider>,
    private val ingestion: CatalogueIngestionUseCase,
    private val runs: IngestionRunRepository,
    private val horizonDays: Long,
    private val pullOnStartup: Boolean,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun onStartup() {
        if (pullOnStartup) pull()
    }

    fun pull() {
        val today = LocalDate.now(clock)
        val query = FeedRequest(today, today.plusDays(horizonDays))

        providers.forEach { provider ->
            runCatching { ingestion.ingest(provider.id, provider.fetch(query)) }
                .onSuccess { log.info("Provider '{}' ingested: {}", provider.id, it) }
                .onFailure { failure ->
                    log.error("Provider '{}' failed", provider.id, failure)
                    runs.save(
                        IngestionRun.start(provider.id, clock.instant()).finish(
                            IngestionStatus.FAILED,
                            clock.instant(),
                            failure.message ?: failure.javaClass.simpleName,
                        ),
                    )
                }
        }
    }
}
