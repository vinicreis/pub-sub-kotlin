package io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper

import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.TextMessages
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.UpdateBuilder

context(Transaction)
val ResultRow.asDomainTextMessage: TextMessage get() = TextMessage(
    id = this[TextMessages.id].value,
    content = this[TextMessages.content],
    queue = this.asDomainQueue
)

context(TextMessages)
infix fun UpdateBuilder<Number>.from(textMessage: TextMessage) {
    this[id] = textMessage.id
    this[content] = textMessage.content
    this[queueId] = textMessage.queue.id
}
