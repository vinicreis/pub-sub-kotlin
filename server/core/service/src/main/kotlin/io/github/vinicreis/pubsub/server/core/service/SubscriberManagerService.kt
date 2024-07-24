package io.github.vinicreis.pubsub.server.core.service

import io.github.vinicreis.pubsub.server.core.model.data.Channel
import io.github.vinicreis.pubsub.server.core.model.data.Message
import kotlinx.coroutines.flow.Flow

interface SubscriberManagerService {
    fun subscribersCount(channel: Channel): Int
    fun subscribe(channel: Channel): Flow<Message>
}
