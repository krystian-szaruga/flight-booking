package dev.maxaure.flightbooking.bootstrap

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@TestConfiguration
internal class MutableClockConfig {

    @Bean
    @Primary
    fun mutableClock(): MutableClock = MutableClock()
}

class MutableClock(private val base: Instant = Instant.now()) : Clock() {

    private var current: Instant = base

    fun advance(duration: Duration) {
        current = current.plus(duration)
    }

    fun reset() {
        current = base
    }

    override fun instant(): Instant = current

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}
