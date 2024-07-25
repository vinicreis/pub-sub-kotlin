package io.github.vinicreis.pubsub.server.data.database.local

import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.model.data.Message
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

class MessageRepositoryLocal(
    private val channelRepository: ChannelRepository,
    private val coroutineContext: CoroutineContext,
) : MessageRepository {
    private val queues = ConcurrentHashMap<Channel, ProducerScope<Message>>()
    private val subscribers = ConcurrentHashMap<Channel, MutableList<Job>>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineContext)
    private val mutex = Mutex()

    private val Channel.subscriberJobs: MutableList<Job>? get() = subscribers[this]

    private suspend fun <E> ProducerScope<E>.sendAll(elements: List<E>) {
        mutex.withLock { elements.forEach { send(it) } }
    }

    override suspend fun add(channel: Channel, message: Message): MessageRepository.Result.Add {
        return try {
            if(channelRepository.getById(channel.id) !is ChannelRepository.Result.GetById.Success) {
                return MessageRepository.Result.Add.QueueNotFound
            }

            queues[channel]?.send(message) ?: run {
                // TODO: Save on database
            }

            MessageRepository.Result.Add.Success
        } catch (e: Exception) {
            MessageRepository.Result.Add.Error(e)
        }
    }

    override suspend fun addAll(channel: Channel, messages: List<Message>): MessageRepository.Result.Add {
        return try {
            if(channelRepository.getById(channel.id) !is ChannelRepository.Result.GetById.Success) {
                return MessageRepository.Result.Add.QueueNotFound
            }

            queues[channel]?.sendAll(messages) ?: run {
                // TODO: Save on database
            }

            MessageRepository.Result.Add.Success
        } catch (e: Exception) {
            MessageRepository.Result.Add.Error(e)
        }
    }

    override suspend fun poll(channel: Channel): MessageRepository.Result.Poll {
        return try {
            queues[channel]?.let { queue ->
                queue.receive().let { message -> MessageRepository.Result.Poll.Success(message) }
            } ?: MessageRepository.Result.Poll.QueueNotFound
        } catch (e: Exception) {
            MessageRepository.Result.Poll.Error(e)
        }
    }

    override suspend fun remove(channel: Channel): MessageRepository.Result.Remove {
        return try {
            queues.remove(channel)?.let { removedQueue ->
                removedQueue.close()

                MessageRepository.Result.Remove.Success
            } ?: MessageRepository.Result.Remove.QueueNotFound
        } catch (e: Exception) {
            MessageRepository.Result.Remove.Error(e)
        }
    }

    override fun subscribe(channel: Channel): MessageRepository.Result.Subscribe {
        return try {
            queues[channel]?.let { queue ->
                MessageRepository.Result.Subscribe.Success(queue)
            } ?: MessageRepository.Result.Subscribe.QueueNotFound
        } catch (e: Exception) {
            MessageRepository.Result.Subscribe.Error(e)
        }
    }
}
