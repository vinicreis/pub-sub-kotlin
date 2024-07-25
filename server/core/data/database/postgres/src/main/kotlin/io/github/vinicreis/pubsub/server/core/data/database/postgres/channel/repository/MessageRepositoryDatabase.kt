package io.github.vinicreis.pubsub.server.core.data.database.postgres.channel.repository

import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.model.data.Message
import io.github.vinicreis.pubsub.server.data.repository.MessageRepository

class MessageRepositoryDatabase : MessageRepository {
    override suspend fun add(channel: Channel, message: Message): MessageRepository.Result.Add {
        TODO("Not yet implemented")
    }

    override suspend fun addAll(channel: Channel, messages: List<Message>): MessageRepository.Result.Add {
        TODO("Not yet implemented")
    }

    override suspend fun poll(channel: Channel): MessageRepository.Result.Poll {
        TODO("Not yet implemented")
    }

    override suspend fun remove(channel: Channel): MessageRepository.Result.Remove {
        TODO("Not yet implemented")
    }

    override fun subscribe(channel: Channel): MessageRepository.Result.Subscribe {
        TODO("Not yet implemented")
    }
}
