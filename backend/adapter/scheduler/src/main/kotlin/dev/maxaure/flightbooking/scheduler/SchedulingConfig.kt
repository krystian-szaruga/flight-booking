package dev.maxaure.flightbooking.scheduler

import dev.maxaure.flightbooking.application.port.`in`.IngestCatalogue
import dev.maxaure.flightbooking.application.port.out.FlightDataProvider
import dev.maxaure.flightbooking.application.port.out.IngestionRunRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler
import java.time.Clock

@Configuration
@EnableScheduling
@ConditionalOnProperty("catalogue.pull.enabled", havingValue = "true")
internal class SchedulingConfig {

    @Bean
    fun taskScheduler(): SimpleAsyncTaskScheduler =
        SimpleAsyncTaskScheduler().apply {
            setVirtualThreads(true)
            setThreadNamePrefix("feed-pull-")
        }

    @Bean
    fun feedPullJob(
        providers: List<FlightDataProvider>,
        catalogueIngestionUseCase: IngestCatalogue,
        ingestionRunRepository: IngestionRunRepository,
        @Value($$"${catalogue.pull.horizon-days}") horizonDays: Long,
        @Value($$"${catalogue.pull.on-startup}") pullOnStartup: Boolean,
    ) = FeedPullJob(
        providers,
        catalogueIngestionUseCase,
        ingestionRunRepository,
        horizonDays,
        pullOnStartup,
        Clock.systemUTC(),
    )

    @Bean
    fun feedPullTrigger(
        job: FeedPullJob,
        scheduler: SimpleAsyncTaskScheduler,
    ) = FeedPullTrigger(job, scheduler)
}
