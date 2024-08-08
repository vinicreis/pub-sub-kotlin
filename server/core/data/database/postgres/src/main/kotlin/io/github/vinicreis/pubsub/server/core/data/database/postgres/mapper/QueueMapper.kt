package io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper

import io.github.vinicreis.pubsub.server.core.data.database.postgres.table.Queues
import io.github.vinicreis.pubsub.server.core.data.database.postgres.table.TextMessages
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.transactions.TransactionInterface

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

context(T)
val <T : TransactionInterface> ResultRow.asDomainQueue: Queue
    get() = Queue(
    id = this[Queues.id].value,
    code = this[Queues.code],
    name = this[Queues.name],
    type = this[Queues.type].asDomain,
    pendingMessagesCount = TextMessages.select(TextMessages.id).where { TextMessages.queueId eq this@asDomainQueue[Queues.id].value }.count()
)
