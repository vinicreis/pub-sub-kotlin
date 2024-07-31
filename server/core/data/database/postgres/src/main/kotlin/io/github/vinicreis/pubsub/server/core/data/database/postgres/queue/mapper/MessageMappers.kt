package io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.mapper

import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.entity.Messages
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction

context(Transaction)
val ResultRow.asDomainTextMessage: TextMessage get() = TextMessage(
    id = this[Messages.id].value,
    content = this[Messages.content]
)