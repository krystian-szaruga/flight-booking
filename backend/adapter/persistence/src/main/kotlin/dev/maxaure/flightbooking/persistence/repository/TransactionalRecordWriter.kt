package dev.maxaure.flightbooking.persistence.repository

import dev.maxaure.flightbooking.application.catalogue.repository.RecordWriter
import org.springframework.transaction.support.TransactionTemplate

internal class TransactionalRecordWriter(
    private val transactions: TransactionTemplate,
) : RecordWriter {

    override fun <T : Any> write(block: () -> T): T = transactions.execute { block() }
        ?: error("TransactionTemplate returned no result")
}
