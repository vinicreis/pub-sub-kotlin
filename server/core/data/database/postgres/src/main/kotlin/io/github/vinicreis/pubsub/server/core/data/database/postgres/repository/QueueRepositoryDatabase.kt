package io.github.vinicreis.pubsub.server.core.data.database.postgres.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.extensions.withExposedTransaction
import io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper.asDomainQueue
import io.github.vinicreis.pubsub.server.core.data.database.postgres.mapper.from
import io.github.vinicreis.pubsub.server.core.data.database.postgres.table.Queues
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueAddedEvent
import io.github.vinicreis.pubsub.server.core.model.data.event.QueueRemovedEvent
import io.github.vinicreis.pubsub.server.data.repository.EventRepository
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionInterface
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class QueueRepositoryDatabase(
    private val coroutineContext: CoroutineContext,
    private val eventRepository: EventRepository,
    private val logger: Logger = Logger.getLogger(QueueRepositoryDatabase::class.java.simpleName)
) : QueueRepository {
    context(T)
    private fun <T : TransactionInterface> selectAllQueues() = Queues.selectAll()

    override suspend fun exists(queue: Queue): Boolean = withContext(coroutineContext) {
        transaction {
            Queues.selectAll().any { it[Queues.id].value == queue.id || it[Queues.code] == queue.code }
        }
    }

    private fun Queue.validate() {
        require(code.isNotBlank()) { "Queue id cannot be blank" }
        require(name.isNotBlank()) { "Queue name cannot be blank" }
    }

    override suspend fun add(queue: Queue): QueueRepository.Result.Add = withContext(coroutineContext) {
        try {
            queue.validate()
            if (exists(queue)) return@withContext QueueRepository.Result.Add.AlreadyFound

            withExposedTransaction {
                Queues.insert { it from queue }
                eventRepository.notify(QueueAddedEvent(queue = queue))
            }

            QueueRepository.Result.Add.Success(queue)
        } catch (e: Exception) {
            logger.severe("Failed to add queue ${queue.id}")
            e.printStackTrace()

            QueueRepository.Result.Add.Error(e)
        }
    }

    override suspend fun removeById(id: UUID): QueueRepository.Result.Remove = withContext(coroutineContext) {
        try {
            withExposedTransaction {
                selectAllQueues()
                    .where { Queues.id eq id }
                    .map { it.asDomainQueue }
                    .firstOrNull()?.also {
                        Queues.deleteWhere { this.id eq id }
                        eventRepository.notify(QueueRemovedEvent(queueId = id))
                    }
            }?.let { removedQueue -> QueueRepository.Result.Remove.Success(removedQueue) }
                ?: QueueRepository.Result.Remove.NotFound
        } catch (e: Exception) {
            logger.severe("Failed to remove queue $id")
            e.printStackTrace()

            QueueRepository.Result.Remove.Error(e)
        }
    }

    override suspend fun getAll(): QueueRepository.Result.GetAll = withContext(coroutineContext) {
        try {
            transaction {
                selectAllQueues().map { it.asDomainQueue }
            }.let { queues -> QueueRepository.Result.GetAll.Success(queues) }
        } catch (e: Exception) {
            logger.severe("Failed to get all queues")
            e.printStackTrace()

            QueueRepository.Result.GetAll.Error(e)
        }
    }

    override suspend fun getById(id: UUID): QueueRepository.Result.GetById = withContext(coroutineContext) {
        try {
            transaction {
                selectAllQueues()
                    .where { Queues.id eq id }
                    .firstOrNull()
                    ?.asDomainQueue
            }?.let { queue ->
                QueueRepository.Result.GetById.Success(queue)
            } ?: QueueRepository.Result.GetById.NotFound
        } catch (e: Exception) {
            logger.severe("Failed to get queue by id")
            e.printStackTrace()

            QueueRepository.Result.GetById.Error(e)
        }
    }
}
