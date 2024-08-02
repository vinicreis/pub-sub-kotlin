package io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper

import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.Events
import io.github.vinicreis.pubsub.server.core.model.data.event.Event
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueAddedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueRemovedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.TextMessageReceivedEvent
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.UpdateBuilder

context(Events)
internal infix fun UpdateBuilder<Number>.from(event: Event.Saveable) = when(event) {
    is TextMessageReceivedEvent -> this from event
    is QueueAddedEvent -> this from event
    is QueueRemovedEvent -> this from event
}

context(Events)
private infix fun UpdateBuilder<Number>.from(event: TextMessageReceivedEvent) {
    this[id] = event.id
    this[Events.type] = Events.Type.TEXT_MESSAGE_RECEIVED
    this[queueId] = event.textMessage.queue.id
    this[textMessageId] = event.textMessage.id
}

context(Events)
private infix fun UpdateBuilder<Number>.from(event: QueueAddedEvent) {
    this[id] = event.id
    this[Events.type] = Events.Type.QUEUE_ADDED
    this[queueId] = event.queue.id
}

context(Events)
private infix fun UpdateBuilder<Number>.from(event: QueueRemovedEvent) {
    this[id] = event.id
    this[Events.type] = Events.Type.QUEUE_REMOVED
    this[queueId] = event.queueId
}

context(Transaction)
internal val ResultRow.asDomainEvent: Event.Saveable
    get() = when(this[Events.type]) {
        Events.Type.TEXT_MESSAGE_RECEIVED -> this.asTextMessageReceivedEvent
        Events.Type.QUEUE_ADDED -> this.asQueueAddedEvent
        Events.Type.QUEUE_REMOVED -> this.asQueueRemovedEvent
    }

context(Transaction)
private val ResultRow.asTextMessageReceivedEvent: TextMessageReceivedEvent
    get() = TextMessageReceivedEvent(
        id = this[Events.id].value,
        textMessage = this.asDomainTextMessage,
        createdAt = this[Events.createdAt]
    )

context(Transaction)
private val ResultRow.asQueueAddedEvent: QueueAddedEvent
    get() = QueueAddedEvent(
        id = this[Events.id].value,
        queue = this.asDomainQueue,
        createdAt = this[Events.createdAt]
    )

context(Transaction)
private val ResultRow.asQueueRemovedEvent: QueueRemovedEvent
    get() = QueueRemovedEvent(
        id = this[Events.id].value,
        queueId = this[Events.queueId],
        createdAt = this[Events.createdAt]
    )
