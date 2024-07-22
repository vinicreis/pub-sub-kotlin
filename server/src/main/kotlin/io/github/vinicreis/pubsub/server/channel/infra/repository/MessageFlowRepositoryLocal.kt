package io.github.vinicreis.pubsub.server.channel.infra.repository

import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.pubsub.server.channel.domain.model.MessageFlow
import io.github.vinicreis.pubsub.server.channel.domain.model.MultipleMessageFlow
import io.github.vinicreis.pubsub.server.channel.domain.model.SimpleMessageFlow
import io.github.vinicreis.pubsub.server.channel.domain.repository.MessageFlowRepository
import java.util.concurrent.ConcurrentHashMap

class MessageFlowRepositoryLocal : MessageFlowRepository {
    private val channels = ConcurrentHashMap<String, MessageFlow>()

    override suspend fun getOrPut(channelId: String, channelType: Channel.Type): MessageFlowRepository.Result.GetOrPut {
        return channels.getOrPut(channelId) {
            when(channelType) {
                Channel.Type.SIMPLE -> SimpleMessageFlow()
                Channel.Type.MULTIPLE -> MultipleMessageFlow()
            }
        }.let { MessageFlowRepository.Result.GetOrPut.Success(it) }
    }
}
