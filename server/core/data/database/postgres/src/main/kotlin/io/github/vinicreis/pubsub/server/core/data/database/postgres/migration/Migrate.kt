package io.github.vinicreis.pubsub.server.core.data.database.postgres.migration

import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.Events
import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.Queues
import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.TextMessages
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction

context(Transaction)
fun createMissingTablesAndObjects() {
    SchemaUtils.createMissingTablesAndColumns(Queues, TextMessages, Events)
}
