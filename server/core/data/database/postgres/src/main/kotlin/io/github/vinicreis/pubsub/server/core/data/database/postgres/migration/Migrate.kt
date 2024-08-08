package io.github.vinicreis.pubsub.server.core.data.database.postgres.migration

import io.github.vinicreis.pubsub.server.core.data.database.postgres.table.Events
import io.github.vinicreis.pubsub.server.core.data.database.postgres.table.Queues
import io.github.vinicreis.pubsub.server.core.data.database.postgres.table.TextMessages
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction

context(Transaction)
fun createMissingTablesAndObjects() {
    SchemaUtils.createMissingTablesAndColumns(Queues, TextMessages, Events)
}
