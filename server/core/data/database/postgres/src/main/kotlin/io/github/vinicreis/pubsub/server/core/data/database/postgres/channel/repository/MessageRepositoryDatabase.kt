package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.repository

import io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.entity.Messages
import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.model.data.Message
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
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
import kotlinx.coroutines.channels.Channel as KotlinChannel

class MessageRepositoryDatabase(
    private val channelRepository: ChannelRepository,
    private val coroutineContext: CoroutineContext,
    private val logger: Logger = Logger.getLogger(MessageRepositoryDatabase::class.java.simpleName)
) : MessageRepository {
    private val queues = ConcurrentHashMap<Channel, KotlinChannel<Message>>()

    private suspend fun <E> KotlinChannel<E>.sendAll(elements: List<E>) {
        elements.forEach { send(it) }
    }

    private suspend fun Channel.notExists(): Boolean =
        channelRepository.getById(id) !is ChannelRepository.Result.GetById.Success

    private fun MutableMap<Channel, KotlinChannel<Message>>.getOrPut(channel: Channel): KotlinChannel<Message> =
        getOrPut(channel) { KotlinChannel(KotlinChannel.UNLIMITED) }

    private fun Message.validate() {
        require(content.isNotBlank()) { "Message content can not be blank" }
        require(content.length <= Message.MAX_CONTENT_LENGTH) {
            "Message content can not be longer than ${Message.MAX_CONTENT_LENGTH} characters"
        }
    }

    private suspend fun <R : MessageRepository.Result> runCatchingErrors(
        block: suspend () -> R,
        error: (Exception) -> R
    ): R = try {
        block()
    } catch (e: IllegalStateException) {
        e.printStackTrace()
        logger.fine(e.message)
        error(e)
    } catch (e: Exception) {
        e.printStackTrace()
        logger.fine(e.message)
        error(RuntimeException(GENERIC_ERROR_MESSAGE))
    }

    override suspend fun add(channel: Channel, message: Message): MessageRepository.Result.Add {
        return runCatchingErrors(
            error = { e -> MessageRepository.Result.Add.Error(e) },
            block = {
                if (channel.notExists()) return@runCatchingErrors MessageRepository.Result.Add.QueueNotFound

                message.validate()
                transaction {
                    Messages.insert {
                        it[id] = UUID.randomUUID()
                        it[this.channel] = channel.id
                        it[content] = message.content
                        it[createdAt] = Date().time
                    }
                }
                queues.getOrPut(channel).send(message)

                MessageRepository.Result.Add.Success
            }
        )
    }

    override suspend fun addAll(channel: Channel, messages: List<Message>): MessageRepository.Result.Add {
        return runCatchingErrors(
            error = { e -> MessageRepository.Result.Add.Error(e) },
            block = {
                if (channel.notExists()) return@runCatchingErrors MessageRepository.Result.Add.QueueNotFound

                messages.forEach { it.validate() }

                transaction {
                    Messages.batchInsert(messages) { message ->
                        this[Messages.id] = UUID.randomUUID()
                        this[Messages.channel] = channel.id
                        this[Messages.content] = message.content
                        this[Messages.createdAt] = Date().time
                    }
                }
                queues.getOrPut(channel).sendAll(messages)

                MessageRepository.Result.Add.Success
            }
        )
    }

    override suspend fun poll(channel: Channel): MessageRepository.Result.Poll {
        return try {
            if (channel.notExists()) return MessageRepository.Result.Poll.QueueNotFound

            val message = queues.getOrPut(channel).receive()
            transaction { Messages.deleteWhere { id eq message.id } }

            MessageRepository.Result.Poll.Success(message)
        } catch (e: Exception) {
            e.printStackTrace()
            logger.fine(e.message)
            MessageRepository.Result.Poll.Error(e)
        }
    }

    override suspend fun remove(channel: Channel): MessageRepository.Result.Remove {
        return try {
            if (channel.notExists()) return MessageRepository.Result.Remove.QueueNotFound

            queues.remove(channel)?.close()
            transaction { Messages.deleteWhere { id eq channel.id } }

            MessageRepository.Result.Remove.Success
        } catch (e: Exception) {
            e.printStackTrace()
            logger.fine(e.message)
            MessageRepository.Result.Remove.Error(e)
        }
    }

    private fun Flow<Message>.deletingFromDatabase(): Flow<Message> =
        flowOn(coroutineContext).onEach { message ->
            transaction { Messages.deleteWhere { id eq message.id } }
        }

    override fun subscribe(channel: Channel): MessageRepository.Result.Subscribe {
        return try {
            queues.getOrPut(channel).let { queue ->
                MessageRepository.Result.Subscribe.Success(queue.receiveAsFlow().deletingFromDatabase())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logger.fine(e.message)
            MessageRepository.Result.Subscribe.Error(e)
        }
    }

    companion object {
        private const val GENERIC_ERROR_MESSAGE = "Something went wrong while processing database operation"
    }
}
