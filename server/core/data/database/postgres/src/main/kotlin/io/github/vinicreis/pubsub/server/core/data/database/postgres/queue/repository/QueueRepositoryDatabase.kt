package io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.entity.Messages
import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.entity.Queues
import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.entity.pendingMessagesCount
import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.mapper.asDomainQueue
import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.mapper.from
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.logging.Logger

class QueueRepositoryDatabase(
    private val logger: Logger = Logger.getLogger(QueueRepositoryDatabase::class.java.simpleName)
) : QueueRepository {
    init {
        transaction {
            check(Queues.exists()) { "Table ${Queues.tableName} does not exist!" }
        }
    }

    context(Transaction)
    private fun selectAllQueues() =
        (Queues leftJoin Messages)
            .select(Queues.id, Queues.code, Queues.name, Queues.type, Queues.pendingMessagesCount)
            .groupBy(Queues.id, Queues.code, Queues.name, Queues.type)

    override suspend fun exists(queue: Queue): Boolean = transaction {
        Queues.selectAll().any { it[Queues.id].value == queue.id || it[Queues.code] == queue.code }
    }

    private fun Queue.validate() {
        require(code.isNotBlank()) { "Queue id cannot be blank" }
        require(name.isNotBlank()) { "Queue name cannot be blank" }
    }

    override suspend fun add(queue: Queue): QueueRepository.Result.Add {
        return try {
            queue.validate()
            if (exists(queue)) return QueueRepository.Result.Add.AlreadyFound

            transaction { Queues.insert { it from queue } }

            QueueRepository.Result.Add.Success(queue)
        } catch (e: Exception) {
            logger.severe("Failed to add queue ${queue.id}")
            e.printStackTrace()

            QueueRepository.Result.Add.Error(e)
        }
    }

    override suspend fun remove(queue: Queue): QueueRepository.Result.Remove = try {
        transaction {
            selectAllQueues()
                .where { Queues.id eq queue.id }
                .map { it.asDomainQueue }
                .firstOrNull()
                ?.also { Queues.deleteWhere { id eq queue.id } }
        }?.let { QueueRepository.Result.Remove.Success(it) }
            ?: QueueRepository.Result.Remove.NotFound
    } catch (e: Exception) {
        logger.severe("Failed to remove queue ${queue.id}")
        e.printStackTrace()

        QueueRepository.Result.Remove.Error(e)
    }

    override suspend fun removeById(id: UUID): QueueRepository.Result.Remove = try {
        transaction {
            selectAllQueues()
                .where { Queues.id eq id }
                .map { it.asDomainQueue }
                .firstOrNull()?.also { Queues.deleteWhere { this.id eq id } }
        }?.let { removedQueue -> QueueRepository.Result.Remove.Success(removedQueue) }
            ?: QueueRepository.Result.Remove.NotFound
    } catch (e: Exception) {
        logger.severe("Failed to remove queue $id")
        e.printStackTrace()

        QueueRepository.Result.Remove.Error(e)
    }

    override suspend fun removeByCode(code: String): QueueRepository.Result.Remove = try {
        transaction {
            selectAllQueues()
                .where { Queues.code eq code }
                .firstOrNull()
                ?.asDomainQueue
                ?.also { Queues.deleteWhere { this.code eq code }.takeIf { it > 0 } }
        }?.let { removedQueue -> QueueRepository.Result.Remove.Success(removedQueue) }
            ?: QueueRepository.Result.Remove.NotFound
    } catch (e: Exception) {
        logger.severe("Failed to remove queue with code $code")
        e.printStackTrace()

        QueueRepository.Result.Remove.Error(e)
    }

    override suspend fun getAll(): QueueRepository.Result.GetAll = try {
        transaction {
            selectAllQueues().map { it.asDomainQueue }
        }.let { queues -> QueueRepository.Result.GetAll.Success(queues) }
    } catch (e: Exception) {
        logger.severe("Failed to get all queues")
        e.printStackTrace()

        QueueRepository.Result.GetAll.Error(e)
    }

    override suspend fun getById(id: UUID): QueueRepository.Result.GetById = try {
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
