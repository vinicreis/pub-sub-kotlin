package io.github.vinicreis.pubsub.server.data.database.local

import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.model.data.Message
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.channels.Channel as KotlinChannel

class MessageRepositoryLocal(
    private val channelRepository: ChannelRepository,
    private val coroutineContext: CoroutineContext,
) : MessageRepository {
    private val queues = ConcurrentHashMap<Channel, KotlinChannel<Message>>()

    private suspend fun <E> KotlinChannel<E>.sendAll(elements: List<E>) {
        elements.forEach { send(it) }
    }

    private suspend fun Channel.notExists(): Boolean =
        channelRepository.getById(id) !is ChannelRepository.Result.GetById.Success

    private fun MutableMap<Channel, KotlinChannel<Message>>.getOrPut(channel: Channel): KotlinChannel<Message> =
        getOrPut(channel) { KotlinChannel(KotlinChannel.UNLIMITED) }

    override suspend fun add(channel: Channel, message: Message): MessageRepository.Result.Add {
        return try {
            if(channel.notExists()) return MessageRepository.Result.Add.QueueNotFound

            // TODO: Save on database
            queues.getOrPut(channel).send(message)

            MessageRepository.Result.Add.Success
        } catch (e: Exception) {
            MessageRepository.Result.Add.Error(e)
        }
    }

    override suspend fun addAll(channel: Channel, messages: List<Message>): MessageRepository.Result.Add {
        return try {
            if(channel.notExists()) return MessageRepository.Result.Add.QueueNotFound

            // TODO: Save on database
            queues.getOrPut(channel).sendAll(messages)

            MessageRepository.Result.Add.Success
        } catch (e: Exception) {
            MessageRepository.Result.Add.Error(e)
        }
    }

    override suspend fun poll(channel: Channel): MessageRepository.Result.Poll {
        return try {
            if(channel.notExists()) return MessageRepository.Result.Poll.QueueNotFound

            val message = queues.getOrPut(channel).receive()
            // TODO: Delete from database

            MessageRepository.Result.Poll.Success(message)
        } catch (e: Exception) {
            MessageRepository.Result.Poll.Error(e)
        }
    }

    override suspend fun remove(channel: Channel): MessageRepository.Result.Remove {
        return try {
            if(channel.notExists()) return MessageRepository.Result.Remove.QueueNotFound

            queues.remove(channel)?.close()
            // TODO: Delete what is on database

            MessageRepository.Result.Remove.Success
        } catch (e: Exception) {
            MessageRepository.Result.Remove.Error(e)
        }
    }

    private fun Flow<Message>.deletingFromDatabase(): Flow<Message> =
        flowOn(coroutineContext).onEach { /* TODO: Delete from database */ }

    override fun subscribe(channel: Channel): MessageRepository.Result.Subscribe {
        return try {
            queues.getOrPut(channel) { KotlinChannel(KotlinChannel.UNLIMITED) }.let { queue ->
                MessageRepository.Result.Subscribe.Success(queue.receiveAsFlow().deletingFromDatabase())
            }
        } catch (e: Exception) {
            MessageRepository.Result.Subscribe.Error(e)
        }
    }
}
