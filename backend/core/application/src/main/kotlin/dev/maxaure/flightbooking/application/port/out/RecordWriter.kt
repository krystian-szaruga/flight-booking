package dev.maxaure.flightbooking.application.port.out

interface RecordWriter {
    fun <T : Any> write(block: () -> T): T
}
