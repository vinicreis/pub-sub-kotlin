package io.github.vinicreis.pubsub.server.core.data.database.postgres.migration

import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.entity.Channels
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction

context(Transaction)
fun createMissingTablesAndObjects() {
    SchemaUtils.createMissingTablesAndColumns(Channels)
}
