package io.github.vinicreis.pubsub.server.core.data.database.postgres.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.Events
import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.Queues
import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.TextMessages
import io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper.asDomainEvent
import io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper.from
import io.github.vinicreis.pubsub.server.core.model.data.event.Event
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueAddedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueRemovedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.data.model.Transaction
import io.github.vinicreis.pubsub.server.data.repository.EventsRepository
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Join
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class EventRepositoryDatabase(
    private val coroutineContext: CoroutineContext,
    private val logger: Logger = Logger.getLogger(EventRepositoryDatabase::class.simpleName)
) : EventsRepository {
    context(T)
    override fun <T : Transaction> notify(event: Event.Saveable) {
        Events.insert { it from event }
    }

    override suspend fun consume(queueId: UUID): Event.Saveable? = withContext(coroutineContext) {
        try {
            transaction {
                Events
                    .withQueues()
                    .withTextMessages()
                    .selectAll()
                    .where { Events.queueId eq queueId }
                    .orderBy(Events.createdAt, SortOrder.ASC)
                    .firstOrNull()
                    ?.asDomainEvent
                    ?.alsoDeleteIfFound()
                    ?.alsoDeleteAssociatedObjects()
            }
        } catch (e: Exception) {
            logger.severe("Failed to query latest queue $queueId event")
            null
        }
    }

    private fun Events.withQueues() = this.join(Queues, JoinType.LEFT, onColumn = queueId, otherColumn = Queues.id)

    private fun Join.withTextMessages() =
        this.join(TextMessages, JoinType.LEFT, onColumn = Events.textMessageId, otherColumn = TextMessages.id)

    private fun <E : Event.Saveable> E?.alsoDeleteIfFound(): E? =
        this?.also { event -> Events.deleteWhere { id eq event.id } }

    private fun <E : Event.Saveable> E?.alsoDeleteAssociatedObjects(): E? = this?.also { event ->
        when (event) {
            is QueueRemovedEvent -> Unit
            is QueueAddedEvent -> Unit
            is TextMessageReceivedEvent -> TextMessages.deleteWhere { id eq event.textMessage.id }
        }
    }
}
