package io.github.vinicreis.pubsub.server.core.data.database.postgres.migration

import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.entity.Messages
import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.entity.Queues
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction

context(Transaction)
fun createMissingTablesAndObjects() {
    SchemaUtils.createMissingTablesAndColumns(Queues, Messages)
}
