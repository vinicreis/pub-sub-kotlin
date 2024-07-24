package io.github.vinicreis.pubsub.server.data.database.postgres.migration

import io.github.vinicreis.pubsub.server.data.database.postgres.channel.entity.Channels
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction

fun Transaction.createMissingTablesAndObjects() {
    SchemaUtils.createMissingTablesAndColumns(Channels)
}