package dev.maxaure.flightbooking.scheduler


import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.Executor

internal class FeedPullTrigger(
    private val job: FeedPullJob,
    private val executor: Executor,
) {

    @EventListener(ApplicationReadyEvent::class)
    fun onReady() {
        executor.execute { job.onStartup() }
    }

    @Scheduled(cron = $$"${catalogue.pull.cron}")
    fun onSchedule() = job.pull()
}
