package io.github.vinicreis.pubsub.server.channel.domain.service

import io.github.vinicreis.pubsub.server.channel.domain.model.Channel
import io.github.vinicreis.pubsub.server.channel.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface SubscriberManager {
    fun subscribe(channel: Channel): Flow<Message>
}