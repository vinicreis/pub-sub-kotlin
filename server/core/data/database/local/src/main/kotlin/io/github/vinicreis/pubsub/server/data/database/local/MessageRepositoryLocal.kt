package io.github.vinicreis.pubsub.server.data.database.local

import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.model.data.Message
import io.github.vinicreis.pubsub.server.data.repository.ChannelRepository
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.Channel as KotlinChannel

class MessageRepositoryLocal(
    private val channelRepository: ChannelRepository
) : MessageRepository {
    private val queues = ConcurrentHashMap<Channel, KotlinChannel<Message>>()

    private fun MutableMap<Channel, KotlinChannel<Message>>.getOrPutNew(key: Channel) =
        getOrPut(key) { KotlinChannel(KotlinChannel.UNLIMITED) }

    override suspend fun add(channel: Channel, message: Message): MessageRepository.Result.Add {
        return try {
            if(channelRepository.getById(channel.id) !is ChannelRepository.Result.GetById.Success) {
                return MessageRepository.Result.Add.QueueNotFound
            }

            queues.getOrPutNew(channel).send(message)

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

            messages.forEach { message -> queues.getOrPutNew(channel).send(message) }

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
