package io.github.vinicreis.pubsub.server.core.data.database.postgres.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.Events
import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.Queues
import io.github.vinicreis.pubsub.server.core.data.database.postgres.entity.TextMessages
import io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper.asDomainEvent
import io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper.from
import io.github.vinicreis.pubsub.server.core.model.data.Event
import io.github.vinicreis.pubsub.server.data.model.Transaction
import io.github.vinicreis.pubsub.server.data.repository.EventsRepository
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.logging.Logger

class EventRepositoryDatabase(
    private val logger: Logger = Logger.getLogger(EventRepositoryDatabase::class.simpleName)
) : EventsRepository {
    context(T)
    override fun <T : Transaction> notify(event: Event) {
        Events.insert { it from event }
    }

    override suspend fun consume(): EventsRepository.Result.Consume =
        try {
            transaction {
                ((Events leftJoin Queues) leftJoin TextMessages)
                    .selectAll()
                    .orderBy(Events.createdAt, SortOrder.ASC)
                    .firstOrNull()
                    ?.asDomainEvent
            }?.let { EventsRepository.Result.Consume.Success(it) }
                ?: EventsRepository.Result.Consume.None
        } catch (e: Exception) {
            logger.severe("Failed to consume next event")
            e.printStackTrace()
            EventsRepository.Result.Consume.Fail(e)
        }
}
