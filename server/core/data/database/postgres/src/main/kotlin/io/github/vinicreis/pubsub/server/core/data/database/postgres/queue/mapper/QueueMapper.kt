package io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.mapper

import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.entity.Queues
import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.entity.pendingMessagesCount
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.UpdateBuilder

internal val Queue.Type.toEntity: Queues.Type
    get() = when (this) {
        Queue.Type.SIMPLE -> Queues.Type.SIMPLE
        Queue.Type.MULTIPLE -> Queues.Type.MULTIPLE
    }

context(Queues)
infix fun UpdateBuilder<Number>.from(queue: Queue) {
    this[id] = queue.id
    this[code] = queue.code
    this[name] = queue.name
    this[Queues.type] = queue.type.toEntity
}

internal val Queues.Type.asDomain: Queue.Type
    get() = when (this) {
        Queues.Type.SIMPLE -> Queue.Type.SIMPLE
        Queues.Type.MULTIPLE -> Queue.Type.MULTIPLE
    }

context(Transaction)
val ResultRow.asDomainQueue: Queue get() = Queue(
    id = this[Queues.id].value,
    code = this[Queues.code],
    name = this[Queues.name],
    type = this[Queues.type].asDomain,
    pendingMessagesCount = this[Queues.pendingMessagesCount]
)
