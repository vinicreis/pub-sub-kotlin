package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.mapper

import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.entity.Messages
import io.github.vinicreis.pubsub.server.core.model.data.Message
import org.jetbrains.exposed.sql.ResultRow

val ResultRow.asDomainMessage: Message get() = Message(
    id = this[Messages.id].value,
    content = this[Messages.content]
)