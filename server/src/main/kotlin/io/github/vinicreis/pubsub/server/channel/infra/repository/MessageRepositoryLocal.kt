package io.github.vinicreis.pubsub.server.channel.infra.repository

import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.pubsub.server.channel.domain.model.Message
import io.github.vinicreis.pubsub.server.channel.domain.repository.MessageRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.Channel as KotlinChannel

class MessageRepositoryLocal : MessageRepository {
    private val queues = ConcurrentHashMap<String, KotlinChannel<Message>>()

    private fun MutableMap<String, KotlinChannel<Message>>.getOrPutNew(key: String) =
        getOrPut(key) { KotlinChannel(KotlinChannel.UNLIMITED) }

    override suspend fun add(channel: Channel, message: Message): MessageRepository.Result.Add {
        return try {
            queues.getOrPutNew(channel.id).send(message)

            MessageRepository.Result.Add.Success
        } catch (e: Exception) {
            MessageRepository.Result.Add.Error(e)
        }
    }

    override suspend fun addAll(channel: Channel, messages: List<Message>): MessageRepository.Result.Add {
        return try {
            messages.forEach { message -> queues.getOrPutNew(channel.id).send(message) }

            MessageRepository.Result.Add.Success
        } catch (e: Exception) {
            MessageRepository.Result.Add.Error(e)
        }
    }

    override suspend fun poll(channel: Channel): MessageRepository.Result.Poll {
        return try {
            queues[channel.id]?.let { queue ->
                queue.receive().let { message -> MessageRepository.Result.Poll.Success(message) }
            } ?: MessageRepository.Result.Poll.ChannelNotFound
        } catch (e: Exception) {
            MessageRepository.Result.Poll.Error(e)
        }
    }

    override suspend fun subscribe(channel: Channel): MessageRepository.Result.Subscribe {
        return try {
            queues[channel.id]?.let { queue ->
                MessageRepository.Result.Subscribe.Success(queue)
            } ?: MessageRepository.Result.Subscribe.ChannelNotFound
        } catch (e: Exception) {
            MessageRepository.Result.Subscribe.Error(e)
        }
    }
}
