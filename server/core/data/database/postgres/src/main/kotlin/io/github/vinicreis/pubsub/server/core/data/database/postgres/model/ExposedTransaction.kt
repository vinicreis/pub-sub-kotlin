package io.github.vinicreis.pubsub.server.core.data.database.postgres.model

import io.github.vinicreis.pubsub.server.data.model.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionInterface

interface ExposedTransaction : Transaction, TransactionInterface
