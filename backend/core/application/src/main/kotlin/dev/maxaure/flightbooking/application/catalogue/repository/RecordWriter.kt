package dev.maxaure.flightbooking.application.catalogue.repository

interface RecordWriter {
    fun <T : Any> write(block: () -> T): T
}
