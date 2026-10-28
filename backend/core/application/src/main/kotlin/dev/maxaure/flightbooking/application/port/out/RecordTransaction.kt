package dev.maxaure.flightbooking.application.port.out

import java.util.UUID
import java.util.function.Consumer

interface RecordTransaction {
    fun inTransaction(action: Runnable)

    fun <T> inTransaction(block: () -> T): T

    fun airportRepository(): AirportRepository

    fun flightRepository(): FlightRepository

    fun onCommit(action: Runnable)

    fun onRollback(action: Consumer<Throwable>)

    fun currentTransactionId(): UUID
}
