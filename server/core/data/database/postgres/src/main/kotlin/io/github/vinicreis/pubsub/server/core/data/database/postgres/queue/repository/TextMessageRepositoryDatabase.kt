package io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.queue.entity.Messages
import io.github.vinicreis.pubsub.server.core.model.data.Queue
import io.github.vinicreis.pubsub.server.core.model.data.TextMessage
import io.github.vinicreis.pubsub.server.data.repository.QueueRepository
import io.github.vinicreis.pubsub.server.data.repository.TextMessageRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class TextMessageRepositoryDatabase(
    private val queueRepository: QueueRepository,
    private val coroutineContext: CoroutineContext,
    private val logger: Logger = Logger.getLogger(TextMessageRepositoryDatabase::class.java.simpleName)
) : TextMessageRepository {
    private val queues = ConcurrentHashMap<UUID, Channel<TextMessage>>()

    private suspend fun <E> Channel<E>.sendAll(elements: List<E>) {
        elements.forEach {
            send(it) }
    }

    private suspend fun Queue.notExists(): Boolean =
        queueRepository.getById(id) !is QueueRepository.Result.GetById.Success

    private fun MutableMap<UUID, Channel<TextMessage>>.getOrPut(queueId: UUID): Channel<TextMessage> =
        getOrPut(queueId) { Channel(Channel.UNLIMITED) }

    private fun TextMessage.validate() {
        require(content.isNotBlank()) { "Message content can not be blank" }
        require(content.length <= TextMessage.MAX_CONTENT_LENGTH) {
            "Message content can not be longer than ${TextMessage.MAX_CONTENT_LENGTH} characters"
        }
    }

    private suspend fun <R : TextMessageRepository.Result> runCatchingErrors(
        block: suspend () -> R,
        error: (Exception) -> R
    ): R = try {
        block()
    } catch (e: IllegalArgumentException) {
        logger.fine(e.message)
        error(e)
    } catch (e: Exception) {
        logger.fine(e.message)
        error(RuntimeException(GENERIC_ERROR_MESSAGE))
    }

    private suspend fun <R : TextMessageRepository.Result> Queue.ifExists(
        errorResult: R,
        block: suspend () -> R
    ): R = if (notExists()) errorResult else block()

    override suspend fun add(queue: Queue, textMessage: TextMessage): TextMessageRepository.Result.Add {
        return runCatchingErrors(
            error = { e -> TextMessageRepository.Result.Add.Error(e) },
            block = {
                queue.ifExists(errorResult = TextMessageRepository.Result.Add.QueueNotFound) {
                    textMessage.validate()

                    transaction {
                        Messages.insert {
                            it[id] = textMessage.id
                            it[this.queueId] = queue.id
                            it[content] = textMessage.content
                            it[createdAt] = Date().time
                        }
                    }

                    queues.getOrPut(queue.id).send(textMessage)

                    TextMessageRepository.Result.Add.Success
                }
            }
        )
    }

    override suspend fun addAll(queue: Queue, textMessages: List<TextMessage>): TextMessageRepository.Result.Add {
        return runCatchingErrors(
            error = { e -> TextMessageRepository.Result.Add.Error(e) },
            block = {
                queue.ifExists(errorResult = TextMessageRepository.Result.Add.QueueNotFound) {
                    textMessages.forEach { it.validate() }

                    transaction {
                        Messages.batchInsert(textMessages) { message ->
                            this[Messages.id] = message.id
                            this[Messages.queueId] = queue.id
                            this[Messages.content] = message.content
                            this[Messages.createdAt] = Date().time
                        }
                    }
                    queues.getOrPut(queue.id).sendAll(textMessages)

                    TextMessageRepository.Result.Add.Success
                }
            }
        )
    }

    override suspend fun remove(queue: Queue): TextMessageRepository.Result.Remove {
        return runCatchingErrors(
            error = { e -> TextMessageRepository.Result.Remove.Error(e) },
            block = {
                queue.ifExists(TextMessageRepository.Result.Remove.QueueNotFound) {
                    queues.remove(queue.id)?.close()

                    transaction { Messages.deleteWhere { Messages.queueId eq queue.id } }

                    TextMessageRepository.Result.Remove.Success
                }
            }
        )
    }

    private fun Flow<TextMessage>.onEachDeleteFromDatabase(): Flow<TextMessage> =
        onEach { message -> transaction { Messages.deleteWhere { id eq message.id } } }

    override suspend fun subscribe(queue: Queue): TextMessageRepository.Result.Subscribe {
        return runCatchingErrors(
            error = { e -> TextMessageRepository.Result.Subscribe.Error(e) },
            block = {
                queue.ifExists(errorResult = TextMessageRepository.Result.Subscribe.QueueNotFound) {
                    queues.getOrPut(queue.id).let { queue ->
                        queue
                            .receiveAsFlow()
                            .onEachDeleteFromDatabase()
                            .flowOn(coroutineContext)
                            .let { flow -> TextMessageRepository.Result.Subscribe.Success(flow) }
                    }
                }
            }
        )
    }

    companion object {
        private const val GENERIC_ERROR_MESSAGE = "Something went wrong while processing database operation"
    }
}
