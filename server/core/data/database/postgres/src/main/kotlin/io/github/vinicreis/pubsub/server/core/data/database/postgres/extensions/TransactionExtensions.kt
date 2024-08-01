package io.github.vinicreis.pubsub.server.core.data.database.postgres.extensions

import io.github.vinicreis.pubsub.server.core.data.database.postgres.model.ExposedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

internal fun <R> withExposedTransaction(block: context(ExposedTransaction) () -> R): R =
    transaction { with(ExposedTransaction(this), block) }
