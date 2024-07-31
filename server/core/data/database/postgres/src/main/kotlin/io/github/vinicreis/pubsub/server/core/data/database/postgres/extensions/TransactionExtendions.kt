package io.github.vinicreis.pubsub.server.core.data.database.postgres.extensions

import io.github.vinicreis.pubsub.server.core.data.database.postgres.model.ExposedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

internal fun <T> withExposedTransaction(block: context(ExposedTransaction) () -> T): T =
    transaction { with(this as ExposedTransaction, block) }
