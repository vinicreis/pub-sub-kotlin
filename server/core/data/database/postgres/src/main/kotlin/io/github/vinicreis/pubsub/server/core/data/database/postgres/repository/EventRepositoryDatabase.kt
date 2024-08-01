package io.github.vinicreis.pubsub.server.core.data.database.postgres.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.Events
import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.Queues
import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.TextMessages
import io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper.asDomainEvent
import io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper.from
import io.github.vinicreis.pubsub.server.core.model.data.event.Event
import io.github.vinicreis.pubsub.server.core.model.data.event.TextMessageReceivedEvent
import io.github.vinicreis.pubsub.server.data.model.Transaction
import io.github.vinicreis.pubsub.server.data.repository.EventsRepository
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.logging.Logger

class EventRepositoryDatabase(
    private val logger: Logger = Logger.getLogger(EventRepositoryDatabase::class.simpleName)
) : EventsRepository {
    context(T)
    override fun <T : Transaction> notify(event: Event.Saveable) {
        Events.insert { it from event }
    }

    override suspend fun consume(queueId: UUID): Event.Saveable? = try {
        transaction {
            ((Events leftJoin Queues) leftJoin TextMessages)
                .selectAll()
                .where { Events.queueId eq queueId }
                .groupBy(Events.id, Queues.id, TextMessages.id)
                .orderBy(Events.createdAt, SortOrder.ASC)
                .firstOrNull()
                ?.also { result -> Events.deleteWhere { id eq result[id] } }
                ?.asDomainEvent
                ?.also { event ->
                    if(event is TextMessageReceivedEvent) {
                        TextMessages.deleteWhere { id eq event.textMessage.id }.also {
                            logger.info("Deleted $it message ${event.textMessage.id}")
                        }
                    }
                }
        }
    } catch (e: Exception) {
        logger.severe("Failed to query latest queue $queueId event")
        null
    }
}
