package io.github.vinicreis.pubsub.server.channel.domain.service

import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.pubsub.server.channel.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface SubscriberManager {
    sealed interface Result {
        sealed interface Subscribe : Result {
            data class Error(val e: Exception) : Subscribe
            data class Success(val messages: Flow<Message>) : Subscribe
            data object ChannelNotFound : Subscribe
        }
    }

    fun subscribe(channel: Channel): Flow<Message>
}