package io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper

import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.Events
import io.github.vinicreis.pubsub.server.core.model.data.Event
import io.github.vinicreis.pubsub.server.core.model.data.TextMessageReceivedEvent
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.UpdateBuilder

context(Events)
internal infix fun UpdateBuilder<Number>.from(event: Event) = when(event) {
    is TextMessageReceivedEvent -> this from event
}

context(Events)
internal infix fun UpdateBuilder<Number>.from(event: TextMessageReceivedEvent) {
    this[id] = event.id
    this[Events.type] = Events.Type.TEXT_MESSAGE_RECEIVED
    this[queueId] = event.queue.id
    this[textMessageId] = event.textMessage.id
}

context(Transaction)
internal val ResultRow.asDomainEvent: Event
    get() = when(this[Events.type]) {
        Events.Type.TEXT_MESSAGE_RECEIVED -> this.asTextMessageReceivedEvent
    }

context(Transaction)
internal val ResultRow.asTextMessageReceivedEvent: TextMessageReceivedEvent
    get() = TextMessageReceivedEvent(
        id = this[Events.id].value,
        queue = this.asDomainQueue,
        textMessage = this.asDomainTextMessage
    )
