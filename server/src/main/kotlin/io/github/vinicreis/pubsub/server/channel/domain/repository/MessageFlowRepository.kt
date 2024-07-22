package io.github.vinicreis.pubsub.server.channel.domain.repository

import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.pubsub.server.channel.domain.model.MessageFlow

interface MessageFlowRepository {
    sealed interface Result {
        sealed interface GetOrPut : Result {
            data class Success(val messageFlow: MessageFlow) : GetOrPut
            data class Error(val e: Exception) : GetOrPut
        }
    }

    suspend fun getOrPut(channelId: String, channelType: Channel.Type): Result.GetOrPut
}
